package drs.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Records that a quantity of a {@link Resource} has been allocated to an
 * {@link Incident}. Marked returned when the allocation ends.
  
 */
public class ResourceAllocation implements Serializable {

    private static final long serialVersionUID = 1L;

    private int allocationPk;
    private String allocationCode;
    private int resourcePk;
    private String resourceCode;          // hydrated for display
    private String resourceName;          // hydrated for display
    private int incidentPk;
    private String incidentCode;          // hydrated for display
    private int quantityAllocated;
    private int allocatedByUserPk;
    private String allocatedByUserCode;   // hydrated for display
    private LocalDateTime allocatedAt;
    private LocalDateTime returnedAt;     // nullable while still allocated
    private String notes;

    public ResourceAllocation() {
        // No-arg for JDBC/Serialization
    }

    public int getAllocationPk() { return allocationPk; }
    public void setAllocationPk(int allocationPk) { this.allocationPk = allocationPk; }

    public String getAllocationCode() { return allocationCode; }
    public void setAllocationCode(String allocationCode) { this.allocationCode = allocationCode; }

    public int getResourcePk() { return resourcePk; }
    public void setResourcePk(int resourcePk) { this.resourcePk = resourcePk; }

    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public int getQuantityAllocated() { return quantityAllocated; }
    public void setQuantityAllocated(int quantityAllocated) { this.quantityAllocated = quantityAllocated; }

    public int getAllocatedByUserPk() { return allocatedByUserPk; }
    public void setAllocatedByUserPk(int allocatedByUserPk) { this.allocatedByUserPk = allocatedByUserPk; }

    public String getAllocatedByUserCode() { return allocatedByUserCode; }
    public void setAllocatedByUserCode(String allocatedByUserCode) { this.allocatedByUserCode = allocatedByUserCode; }

    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isReturned() {
        return returnedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceAllocation)) return false;
        return allocationPk == ((ResourceAllocation) o).allocationPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocationPk);
    }

    @Override
    public String toString() {
        return "ResourceAllocation{" + allocationCode + ", " + quantityAllocated
             + " units of " + resourceCode + " -> " + incidentCode + "}";
    }
}
