package drs.server.service;

import drs.server.repository.DatabaseConnection;
import drs.server.repository.IncidentRepository;
import drs.server.repository.LocationRepository;
import drs.server.repository.ResourceAllocationRepository;
import drs.server.repository.ResourceRepository;
import drs.server.repository.UserRepository;
import drs.server.util.IdGenerator;
import drs.shared.enums.ResourceStatus;
import drs.shared.enums.ResourceType;
import drs.shared.exception.DataAccessException;
import drs.shared.exception.ValidationException;
import drs.shared.model.Incident;
import drs.shared.model.Location;
import drs.shared.model.Resource;
import drs.shared.model.ResourceAllocation;
import drs.shared.model.User;
import drs.shared.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the resource inventory and its allocations to incidents.
 *
 * The {@link #allocateResource} method uses an atomic
 * {@code UPDATE...WHERE quantity_available >= ?} on the resources row
 * inside a JDBC transaction, so two coordinators allocating the same
 * resource concurrently can never drive quantity_available below zero.
  
 */
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final AuditService auditService;

    public ResourceService(ResourceRepository resourceRepository,
                           ResourceAllocationRepository allocationRepository,
                           IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           LocationRepository locationRepository,
                           AuditService auditService) {
        this.resourceRepository = resourceRepository;
        this.allocationRepository = allocationRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.auditService = auditService;
    }

    /*   * Register a new resource in the inventory (admin only - enforced
     * at the handler layer).
         * @param resourceName     human-readable name
     * @param type             category
     * @param quantityTotal    how many units exist
     * @param homeLocationCode where it lives normally (nullable)
     * @param addedByUserCode  acting admin
     * @param clientIp         remote address
     * @return persisted Resource
     */
    public Resource addResource(String resourceName, ResourceType type,
                                int quantityTotal, String homeLocationCode,
                                String addedByUserCode, String clientIp) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new ValidationException("Resource name is required.");
        }
        if (type == null) {
            throw new ValidationException("Resource type is required.");
        }
        if (quantityTotal <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }
        User addedBy = userRepository.findByCode(addedByUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));
        Integer homeLocationPk = null;
        if (homeLocationCode != null && !homeLocationCode.isEmpty()) {
            Location loc = locationRepository.findByCode(homeLocationCode)
                    .orElseThrow(() -> new ValidationException(
                            "Location not found: " + homeLocationCode));
            homeLocationPk = loc.getLocationPk();
        }

        Resource r = new Resource(IdGenerator.generateResourceCode(),
                resourceName.trim(), type, quantityTotal, homeLocationPk);
        resourceRepository.save(r);

        auditService.logAction(addedBy.getUserPk(), "ADD_RESOURCE",
                "Resource", r.getResourceCode(),
                resourceName + " x" + quantityTotal, clientIp, true);
        return r;
    }

    public List<Resource> listResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> listAvailableResources(ResourceType type) {
        if (type == null) {
            return resourceRepository.findAll();
        }
        return resourceRepository.findByType(type);
    }

    /*   * Atomically allocate a quantity of a resource to an incident.
         * Transaction steps:
     *  1. UPDATE resources SET quantity_available = quantity_available - ?
     *     WHERE resource_pk = ? AND quantity_available >= ?
     *  2. If step 1 affected 0 rows: rollback, throw ValidationException.
     *  3. INSERT INTO resource_allocations ...
     *  4. Commit.
         * @param resourceCode resource to allocate from
     * @param incidentCode incident to allocate to
     * @param quantity     how many units (must be > 0)
     * @param userCode     acting coordinator
     * @param notes        optional notes
     * @param clientIp     remote address
     * @return the persisted ResourceAllocation
     */
    public ResourceAllocation allocateResource(String resourceCode,
                                               String incidentCode,
                                               int quantity, String userCode,
                                               String notes, String clientIp) {
        if (!InputValidator.validateQuantity(quantity)) {
            throw new ValidationException("Quantity must be at least 1.");
        }
        Resource resource = resourceRepository.findByCode(resourceCode)
                .orElseThrow(() -> new ValidationException(
                        "Resource not found: " + resourceCode));
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        User user = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        if (quantity > resource.getQuantityAvailable()) {
            throw new ValidationException("Only "
                    + resource.getQuantityAvailable()
                    + " units of " + resource.getResourceName()
                    + " are currently available.");
        }

        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setAllocationCode(IdGenerator.generateAllocationCode());
        allocation.setResourcePk(resource.getResourcePk());
        allocation.setIncidentPk(incident.getIncidentPk());
        allocation.setQuantityAllocated(quantity);
        allocation.setAllocatedByUserPk(user.getUserPk());
        allocation.setAllocatedAt(LocalDateTime.now());
        allocation.setNotes(notes);

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean decremented = resourceRepository.decrementAvailable(
                        c, resource.getResourcePk(), quantity);
                if (!decremented) {
                    c.rollback();
                    throw new ValidationException(
                            "Insufficient quantity available. Try refreshing.");
                }
                allocationRepository.save(c, allocation);
                c.commit();
            } catch (Exception inner) {
                c.rollback();
                if (inner instanceof ValidationException) {
                    throw (ValidationException) inner;
                }
                throw new DataAccessException("allocateResource failed", inner);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("allocateResource transaction failed", e);
        }

        auditService.logAction(user.getUserPk(), "ALLOCATE_RESOURCE",
                "ResourceAllocation", allocation.getAllocationCode(),
                quantity + " x " + resource.getResourceCode()
                        + " -> " + incident.getIncidentCode(),
                clientIp, true);
        return allocation;
    }

    /*   * Return an allocation - restores quantity to the resource pool.
         * @param allocationCode the allocation to return
     * @param userCode       acting user
     * @param clientIp       remote address
     */
    public void returnAllocation(String allocationCode, String userCode,
                                 String clientIp) {
        ResourceAllocation allocation = allocationRepository
                .findByCode(allocationCode)
                .orElseThrow(() -> new ValidationException(
                        "Allocation not found: " + allocationCode));
        if (allocation.isReturned()) {
            throw new ValidationException(
                    "This allocation has already been returned.");
        }
        User user = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                resourceRepository.incrementAvailable(c,
                        allocation.getResourcePk(),
                        allocation.getQuantityAllocated());
                allocationRepository.markReturned(c,
                        allocation.getAllocationPk(), now);
                c.commit();
            } catch (Exception inner) {
                c.rollback();
                throw new DataAccessException("returnAllocation failed", inner);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "returnAllocation transaction failed", e);
        }

        auditService.logAction(user.getUserPk(), "RETURN_ALLOCATION",
                "ResourceAllocation", allocation.getAllocationCode(),
                allocation.getQuantityAllocated() + " units returned",
                clientIp, true);
    }

    public List<ResourceAllocation> listAllocationsForIncident(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        return allocationRepository.findByIncidentPk(incident.getIncidentPk());
    }

    /*   * @param resourceCode resource code
     * @return resource by code
     */
    public Resource getResource(String resourceCode) {
        return resourceRepository.findByCode(resourceCode)
                .orElseThrow(() -> new ValidationException(
                        "Resource not found: " + resourceCode));
    }

    // -----------------------------------------------------------------
    // Lifecycle: move resources between AVAILABLE, MAINTENANCE, RETIRED
    // -----------------------------------------------------------------

    /*   * Send a resource for maintenance. Only valid from AVAILABLE.
     * Resources with active allocations cannot be sent - they must be
     * returned first.
     */
    public Resource sendToMaintenance(String resourceCode,
                                      String actorUserCode,
                                      String clientIp) {
        Resource r = getResource(resourceCode);
        if (r.getStatus() != drs.shared.enums.ResourceStatus.AVAILABLE) {
            throw new ValidationException(
                    "Only AVAILABLE resources can be sent to maintenance "
                            + "(current status: " + r.getStatus() + ")");
        }
        if (r.getQuantityAvailable() < r.getQuantityTotal()) {
            throw new ValidationException(
                    "This resource has active allocations. Return "
                            + "them before sending to maintenance.");
        }
        resourceRepository.updateStatus(r.getResourcePk(),
                drs.shared.enums.ResourceStatus.MAINTENANCE);
        auditService.logAction(null, "SEND_RESOURCE_TO_MAINTENANCE",
                "Resource", r.getResourceCode(),
                "By " + actorUserCode, clientIp, true);
        r.setStatus(drs.shared.enums.ResourceStatus.MAINTENANCE);
        return r;
    }

    /*   * Return a resource from maintenance to AVAILABLE.
     */
    public Resource returnFromMaintenance(String resourceCode,
                                          String actorUserCode,
                                          String clientIp) {
        Resource r = getResource(resourceCode);
        if (r.getStatus() != drs.shared.enums.ResourceStatus.MAINTENANCE) {
            throw new ValidationException(
                    "Only MAINTENANCE resources can be returned "
                            + "(current status: " + r.getStatus() + ")");
        }
        resourceRepository.updateStatus(r.getResourcePk(),
                drs.shared.enums.ResourceStatus.AVAILABLE);
        auditService.logAction(null, "RETURN_RESOURCE_FROM_MAINTENANCE",
                "Resource", r.getResourceCode(),
                "By " + actorUserCode, clientIp, true);
        r.setStatus(drs.shared.enums.ResourceStatus.AVAILABLE);
        return r;
    }

    /*   * Permanently retire a resource. Row stays in the database so
     * allocation history remains intact; the status flag flips to
     * RETIRED and the resource no longer appears in allocate-resource
     * dropdowns.
     */
    public Resource retireResource(String resourceCode,
                                   String actorUserCode,
                                   String clientIp) {
        Resource r = getResource(resourceCode);
        if (r.getStatus() == drs.shared.enums.ResourceStatus.RETIRED) {
            throw new ValidationException(
                    "Resource is already retired.");
        }
        if (r.getQuantityAvailable() < r.getQuantityTotal()) {
            throw new ValidationException(
                    "This resource has active allocations. Return "
                            + "them before retiring.");
        }
        resourceRepository.updateStatus(r.getResourcePk(),
                drs.shared.enums.ResourceStatus.RETIRED);
        auditService.logAction(null, "RETIRE_RESOURCE", "Resource",
                r.getResourceCode(),
                "By " + actorUserCode, clientIp, true);
        r.setStatus(drs.shared.enums.ResourceStatus.RETIRED);
        return r;
    }
}
