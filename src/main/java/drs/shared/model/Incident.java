package drs.shared.model;

import drs.shared.enums.CapCertainty;
import drs.shared.enums.CapSeverity;
import drs.shared.enums.CapUrgency;
import drs.shared.enums.DisasterType;
import drs.shared.enums.IncidentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A reported disaster incident. The central domain entity of the system.
 *
 * The {@code description} field is plaintext in the Java object but
 * stored encrypted in the database (AES-GCM). The
 * {@link drs.server.repository.IncidentRepository} handles the
 * encryption/decryption transparently.
 *
 * Lifecycle states are enforced by
 * {@code drs.shared.state.IncidentStateRegistry}.
  
 */
public class Incident implements Serializable {

    private static final long serialVersionUID = 1L;

    private int incidentPk;
    private String incidentCode;
    private int reportedByUserPk;
    private String reportedByUserCode;   // hydrated for display
    private String reportedByUserName;   // hydrated for display
    private DisasterType disasterType;
    private int locationPk;
    private String locationDisplayName;  // hydrated for display
    /** Hydrated for display: name of the currently-assigned response
     * team (PRIMARY role). Null when no team is assigned yet. */
    private String assignedTeamName;
    /** Hydrated for display: full name of that team's leader.
     * Null when no team is assigned or the team has no leader. */
    private String assignedLeaderName;
    private double incidentLat;
    private double incidentLon;
    private String description;          // plaintext in Java
    /*   * Mandatory callback phone the citizen supplies when reporting.
     * Validated as Australian format (mobile or landline) at the
     * service layer. Stored in cleartext - phone is not considered
     * highly sensitive PII in this domain and the coordinator needs
     * to read it fast.
     */
    private String contactPhone;
    private int peopleAffected;
    private String propertyRiskLevel;
    private CapSeverity capSeverity;
    private CapUrgency capUrgency;
    private CapCertainty capCertainty;
    private int priorityScore;
    private IncidentStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime assessedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String closedBy;

    public Incident() {
        // No-arg for JDBC/Serialization
        this.capSeverity = CapSeverity.UNKNOWN;
        this.capUrgency = CapUrgency.UNKNOWN;
        this.capCertainty = CapCertainty.UNKNOWN;
        this.status = IncidentStatus.REPORTED;
    }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public int getReportedByUserPk() { return reportedByUserPk; }
    public void setReportedByUserPk(int reportedByUserPk) { this.reportedByUserPk = reportedByUserPk; }

    public String getReportedByUserCode() { return reportedByUserCode; }
    public void setReportedByUserCode(String reportedByUserCode) { this.reportedByUserCode = reportedByUserCode; }

    public String getReportedByUserName() { return reportedByUserName; }
    public void setReportedByUserName(String reportedByUserName) { this.reportedByUserName = reportedByUserName; }

    public DisasterType getDisasterType() { return disasterType; }
    public void setDisasterType(DisasterType disasterType) { this.disasterType = disasterType; }

    public int getLocationPk() { return locationPk; }
    public void setLocationPk(int locationPk) { this.locationPk = locationPk; }

    public String getLocationDisplayName() { return locationDisplayName; }
    public void setLocationDisplayName(String locationDisplayName) { this.locationDisplayName = locationDisplayName; }

    public String getAssignedTeamName() { return assignedTeamName; }
    public void setAssignedTeamName(String assignedTeamName) { this.assignedTeamName = assignedTeamName; }

    public String getAssignedLeaderName() { return assignedLeaderName; }
    public void setAssignedLeaderName(String assignedLeaderName) { this.assignedLeaderName = assignedLeaderName; }

    public double getIncidentLat() { return incidentLat; }
    public void setIncidentLat(double incidentLat) { this.incidentLat = incidentLat; }

    public double getIncidentLon() { return incidentLon; }
    public void setIncidentLon(double incidentLon) { this.incidentLon = incidentLon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public int getPeopleAffected() { return peopleAffected; }
    public void setPeopleAffected(int peopleAffected) { this.peopleAffected = peopleAffected; }

    public String getPropertyRiskLevel() { return propertyRiskLevel; }
    public void setPropertyRiskLevel(String propertyRiskLevel) { this.propertyRiskLevel = propertyRiskLevel; }

    public CapSeverity getCapSeverity() { return capSeverity; }
    public void setCapSeverity(CapSeverity capSeverity) { this.capSeverity = capSeverity; }

    public CapUrgency getCapUrgency() { return capUrgency; }
    public void setCapUrgency(CapUrgency capUrgency) { this.capUrgency = capUrgency; }

    public CapCertainty getCapCertainty() { return capCertainty; }
    public void setCapCertainty(CapCertainty capCertainty) { this.capCertainty = capCertainty; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Incident)) return false;
        return incidentPk == ((Incident) o).incidentPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentPk);
    }

    @Override
    public String toString() {
        return "Incident{" + incidentCode + ", " + disasterType + ", "
             + status + ", score=" + priorityScore + "}";
    }
}
