package drs.server.service;

import drs.server.repository.IncidentRepository;
import drs.server.repository.IncidentUpdateRepository;
import drs.server.repository.LocationRepository;
import drs.server.repository.UserRepository;
import drs.server.repository.DatabaseConnection;
import drs.server.util.IdGenerator;
import drs.shared.enums.CapCertainty;
import drs.shared.enums.CapSeverity;
import drs.shared.enums.CapUrgency;
import drs.shared.enums.DisasterType;
import drs.shared.enums.IncidentStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.exception.ValidationException;
import drs.shared.model.Incident;
import drs.shared.model.IncidentUpdate;
import drs.shared.model.Location;
import drs.shared.model.User;
import drs.shared.state.IncidentStateRegistry;
import drs.shared.util.InputValidator;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Owns the lifecycle of {@link Incident} entities - reporting,
 * assessment, status transitions, closure. Delegates state validation
 * to {@link IncidentStateRegistry} and priority computation to
 * {@link PriorityService}.
  
 */
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentUpdateRepository updateRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PriorityService priorityService;
    private final AuditService auditService;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentUpdateRepository updateRepository,
                           UserRepository userRepository,
                           LocationRepository locationRepository,
                           PriorityService priorityService,
                           AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.updateRepository = updateRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.priorityService = priorityService;
        this.auditService = auditService;
    }

    /*   * Create a new incident in REPORTED state.
         * @param reporterUserCode citizen / user reporting it
     * @param disasterType     the kind of disaster
     * @param locationCode     where it is happening
     * @param incidentLat      precise latitude
     * @param incidentLon      precise longitude
     * @param description      free-text description (will be encrypted)
     * @param peopleAffected   estimate (non-negative)
     * @param propertyRiskLevel "Low" / "Moderate" / "High" (nullable)
     * @param clientIp         remote address (for audit)
     * @return the persisted Incident with assigned code and pk
     * @throws ValidationException if any field fails validation
     */
    public Incident reportIncident(String reporterUserCode,
                                   DisasterType disasterType,
                                   String locationCode,
                                   double incidentLat,
                                   double incidentLon,
                                   String description,
                                   String contactPhone,
                                   int peopleAffected,
                                   String propertyRiskLevel,
                                   String clientIp) {
        if (disasterType == null) {
            throw new ValidationException("Please choose a disaster type.");
        }
        if (!InputValidator.validateDescription(description)) {
            throw new ValidationException("Description must be 5–1000 characters.");
        }
        if (!InputValidator.isValidAuPhone(contactPhone)) {
            throw new ValidationException(
                    "Contact phone must be a valid Australian mobile or "
                    + "landline (e.g. 0412 345 678 or 02 9876 5432). "
                    + "This is mandatory so the coordinator can call back.");
        }
        if (!InputValidator.validateLatitude(incidentLat)
                || !InputValidator.validateLongitude(incidentLon)) {
            throw new ValidationException("Coordinates are out of range.");
        }
        if (peopleAffected < 0) {
            throw new ValidationException("People affected cannot be negative.");
        }
        User reporter = userRepository.findByCode(reporterUserCode)
                .orElseThrow(() -> new ValidationException(
                        "Reporter user not found: " + reporterUserCode));
        Location location = locationRepository.findByCode(locationCode)
                .orElseThrow(() -> new ValidationException(
                        "Location not found: " + locationCode));

        Incident incident = new Incident();
        incident.setIncidentCode(IdGenerator.generateIncidentCode());
        incident.setReportedByUserPk(reporter.getUserPk());
        incident.setDisasterType(disasterType);
        incident.setLocationPk(location.getLocationPk());
        incident.setIncidentLat(incidentLat);
        incident.setIncidentLon(incidentLon);
        incident.setDescription(description);
        incident.setContactPhone(contactPhone.trim());
        incident.setPeopleAffected(peopleAffected);
        incident.setPropertyRiskLevel(propertyRiskLevel);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setReportedAt(LocalDateTime.now());
        incident.setPriorityScore(priorityService.calculateScore(incident));

        incidentRepository.save(incident);

        auditService.logAction(reporter.getUserPk(), "REPORT_INCIDENT",
                "Incident", incident.getIncidentCode(),
                disasterType + " at " + location.getDisplayName(),
                clientIp, true);
        return incident;
    }

    /*   * Move REPORTED -> ASSESSED, setting the CAP fields.
     */
    public Incident assessIncident(String incidentCode,
                                   CapSeverity severity,
                                   CapUrgency urgency,
                                   CapCertainty certainty,
                                   String assessorUserCode,
                                   String clientIp) {
        Incident incident = requireIncident(incidentCode);
        IncidentStateRegistry.validateTransition(
                incident.getStatus(), IncidentStatus.ASSESSED);
        User assessor = userRepository.findByCode(assessorUserCode)
                .orElseThrow(() -> new ValidationException(
                        "Assessor user not found"));

        IncidentStatus before = incident.getStatus();
        incident.setCapSeverity(severity);
        incident.setCapUrgency(urgency);
        incident.setCapCertainty(certainty);
        incident.setStatus(IncidentStatus.ASSESSED);
        incident.setAssessedAt(LocalDateTime.now());
        incident.setPriorityScore(priorityService.calculateScore(incident));
        incidentRepository.update(incident);

        recordUpdate(incident.getIncidentPk(), assessor.getUserPk(),
                before, IncidentStatus.ASSESSED,
                "Assessed: " + severity + "/" + urgency + "/" + certainty);

        auditService.logAction(assessor.getUserPk(), "ASSESS_INCIDENT",
                "Incident", incident.getIncidentCode(),
                "score=" + incident.getPriorityScore(), clientIp, true);
        return incident;
    }

    /*   * REPORTED|ASSESSED -> REJECTED with a reason.
     */
    public Incident rejectIncident(String incidentCode, String userCode,
                                   String reason, String clientIp) {
        // Reject requires a substantive reason. Both reporters and
        // auditors need to understand why a report was turned away;
        // empty/short reasons defeat that.
        if (reason == null || reason.trim().length() < 10) {
            throw new ValidationException(
                    "Please provide a rejection reason of at least "
                            + "10 characters.");
        }
        Incident incident = requireIncident(incidentCode);
        IncidentStateRegistry.validateTransition(
                incident.getStatus(), IncidentStatus.REJECTED);
        User actor = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        IncidentStatus before = incident.getStatus();
        incident.setStatus(IncidentStatus.REJECTED);
        incident.setClosedAt(LocalDateTime.now());
        incident.setClosedBy(userCode);
        incidentRepository.update(incident);

        recordUpdate(incident.getIncidentPk(), actor.getUserPk(),
                before, IncidentStatus.REJECTED, reason.trim());

        auditService.logAction(actor.getUserPk(), "REJECT_INCIDENT",
                "Incident", incident.getIncidentCode(), reason.trim(),
                clientIp, true);
        return incident;
    }

    /*   * Citizen-initiated withdrawal of an incident that hasn't yet
     * been assessed. Only the original reporter may withdraw their
     * own report, and only while it is still in REPORTED state.
         * WITHDRAWN is a terminal state distinct from REJECTED: the
     * report wasn't rejected by anyone, the citizen simply chose to
     * retract it. A short reason (min 10 characters) is required so
     * auditors can see why.
         * @param incidentCode the incident to withdraw
     * @param userCode     the citizen attempting the withdrawal
     * @param reason       why they are withdrawing (≥10 chars)
     * @param clientIp     citizen's client IP, for audit
     * @return the updated incident, now in WITHDRAWN state
     * @throws ValidationException if reason is missing/too short or
     *        the actor is not the original reporter
     * @throws InvalidStateTransitionException if status is not REPORTED
     */
    public Incident withdrawIncident(String incidentCode, String userCode,
                                     String reason, String clientIp) {
        if (reason == null || reason.trim().length() < 10) {
            throw new ValidationException(
                    "Please give a brief reason for withdrawing "
                            + "(at least 10 characters).");
        }
        Incident incident = requireIncident(incidentCode);
        IncidentStateRegistry.validateTransition(
                incident.getStatus(), IncidentStatus.WITHDRAWN);

        User actor = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Ownership check: only the original reporter may withdraw.
        if (incident.getReportedByUserPk() != actor.getUserPk()) {
            auditService.logAction(actor.getUserPk(),
                    "WITHDRAW_DENIED", "Incident",
                    incident.getIncidentCode(),
                    "Not the original reporter", clientIp, false);
            throw new ValidationException(
                    "You can only withdraw reports you submitted yourself.");
        }

        IncidentStatus before = incident.getStatus();
        incident.setStatus(IncidentStatus.WITHDRAWN);
        incident.setClosedAt(LocalDateTime.now());
        incident.setClosedBy(userCode);
        incidentRepository.update(incident);

        recordUpdate(incident.getIncidentPk(), actor.getUserPk(),
                before, IncidentStatus.WITHDRAWN, reason.trim());

        auditService.logAction(actor.getUserPk(), "WITHDRAW_INCIDENT",
                "Incident", incident.getIncidentCode(),
                reason.trim(), clientIp, true);
        return incident;
    }

    /*   * Move an incident through one valid status transition.
     */
    public Incident transitionStatus(String incidentCode,
                                     IncidentStatus newStatus,
                                     String userCode,
                                     String comment,
                                     String clientIp) {
        Incident incident = requireIncident(incidentCode);
        IncidentStateRegistry.validateTransition(incident.getStatus(), newStatus);
        User actor = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        IncidentStatus before = incident.getStatus();
        incident.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(now);
        }
        if (newStatus == IncidentStatus.CLOSED) {
            incident.setClosedAt(now);
            incident.setClosedBy(userCode);
        }
        incidentRepository.update(incident);

        recordUpdate(incident.getIncidentPk(), actor.getUserPk(),
                before, newStatus, comment);

        auditService.logAction(actor.getUserPk(),
                "TRANSITION_" + newStatus.name(),
                "Incident", incident.getIncidentCode(), comment,
                clientIp, true);
        return incident;
    }

    /*   * RESOLVED -> CLOSED.
     */
    public Incident closeIncident(String incidentCode, String userCode,
                                  String clientIp) {
        return transitionStatus(incidentCode, IncidentStatus.CLOSED,
                userCode, "Incident closed", clientIp);
    }

    public List<Incident> getAllIncidents() {
        List<Incident> incidents = incidentRepository.findAll();
        return priorityService.sortByPriority(incidents);
    }

    public Optional<Incident> getIncident(String incidentCode) {
        return incidentRepository.findByCode(incidentCode);
    }

    public List<Incident> getIncidentsByReporter(String reporterUserCode) {
        User reporter = userRepository.findByCode(reporterUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));
        return incidentRepository.findByReportedBy(reporter.getUserPk());
    }

    /*   * Recompute the priority score for every incident using the current
     * strategy and persist the new value.
     */
    public void recalculateAllPriorities() {
        for (Incident i : incidentRepository.findAll()) {
            int newScore = priorityService.calculateScore(i);
            if (newScore != i.getPriorityScore()) {
                incidentRepository.updatePriorityScore(i.getIncidentPk(), newScore);
            }
        }
    }

    private Incident requireIncident(String incidentCode) {
        return incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
    }

    private void recordUpdate(int incidentPk, int updatedByPk,
                              IncidentStatus before, IncidentStatus after,
                              String comment) {
        IncidentUpdate u = new IncidentUpdate();
        u.setUpdateCode(IdGenerator.generateUpdateCode());
        u.setIncidentPk(incidentPk);
        u.setUpdatedByPk(updatedByPk);
        u.setStatusBefore(before);
        u.setStatusAfter(after);
        u.setComment(comment);
        u.setCreatedAt(LocalDateTime.now());
        // The update is logged in a fresh connection - no transaction
        // needed because the parent incident update has already been
        // persisted by the caller.
        try (Connection c = DatabaseConnection.getConnection()) {
            updateRepository.save(c, u);
        } catch (java.sql.SQLException e) {
            throw new DataAccessException("Failed to record incident update", e);
        }
    }
}
