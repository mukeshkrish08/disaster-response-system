package drs.shared.model;

import drs.shared.enums.IncidentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A historical record of one state transition on an incident - captures
 * who changed it, from what to what, and an optional comment.
  
 */
public class IncidentUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private int updatePk;
    private String updateCode;
    private int incidentPk;
    private int updatedByPk;
    private String updatedByUserCode;   // hydrated for display
    private IncidentStatus statusBefore;
    private IncidentStatus statusAfter;
    private String comment;
    private LocalDateTime createdAt;

    public IncidentUpdate() {
        // No-arg for JDBC/Serialization
    }

    public int getUpdatePk() { return updatePk; }
    public void setUpdatePk(int updatePk) { this.updatePk = updatePk; }

    public String getUpdateCode() { return updateCode; }
    public void setUpdateCode(String updateCode) { this.updateCode = updateCode; }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public int getUpdatedByPk() { return updatedByPk; }
    public void setUpdatedByPk(int updatedByPk) { this.updatedByPk = updatedByPk; }

    public String getUpdatedByUserCode() { return updatedByUserCode; }
    public void setUpdatedByUserCode(String updatedByUserCode) { this.updatedByUserCode = updatedByUserCode; }

    public IncidentStatus getStatusBefore() { return statusBefore; }
    public void setStatusBefore(IncidentStatus statusBefore) { this.statusBefore = statusBefore; }

    public IncidentStatus getStatusAfter() { return statusAfter; }
    public void setStatusAfter(IncidentStatus statusAfter) { this.statusAfter = statusAfter; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IncidentUpdate)) return false;
        return updatePk == ((IncidentUpdate) o).updatePk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(updatePk);
    }

    @Override
    public String toString() {
        return "IncidentUpdate{" + statusBefore + " -> " + statusAfter
             + " by " + updatedByUserCode + "}";
    }
}
