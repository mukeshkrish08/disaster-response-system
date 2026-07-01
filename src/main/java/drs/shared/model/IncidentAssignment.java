package drs.shared.model;

import drs.shared.enums.AssignmentRole;
import drs.shared.enums.AssignmentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Links a {@link ResponseTeam} to an {@link Incident} with a role
 * (primary / secondary / support) and an assignment status.
  
 */
public class IncidentAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int assignmentPk;
    private String assignmentCode;
    private int incidentPk;
    private String incidentCode;        // hydrated for display
    private int teamPk;
    private String teamCode;            // hydrated for display
    private String teamName;            // hydrated for display
    private AssignmentRole role;
    private AssignmentStatus assignmentStatus;
    private int assignedByUserPk;
    private String assignedByUserCode;  // hydrated for display
    private double distanceKm;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public IncidentAssignment() {
        // No-arg for JDBC/Serialization
    }

    public int getAssignmentPk() { return assignmentPk; }
    public void setAssignmentPk(int assignmentPk) { this.assignmentPk = assignmentPk; }

    public String getAssignmentCode() { return assignmentCode; }
    public void setAssignmentCode(String assignmentCode) { this.assignmentCode = assignmentCode; }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public int getTeamPk() { return teamPk; }
    public void setTeamPk(int teamPk) { this.teamPk = teamPk; }

    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public AssignmentRole getRole() { return role; }
    public void setRole(AssignmentRole role) { this.role = role; }

    public AssignmentStatus getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(AssignmentStatus assignmentStatus) { this.assignmentStatus = assignmentStatus; }

    public int getAssignedByUserPk() { return assignedByUserPk; }
    public void setAssignedByUserPk(int assignedByUserPk) { this.assignedByUserPk = assignedByUserPk; }

    public String getAssignedByUserCode() { return assignedByUserCode; }
    public void setAssignedByUserCode(String assignedByUserCode) { this.assignedByUserCode = assignedByUserCode; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IncidentAssignment)) return false;
        return assignmentPk == ((IncidentAssignment) o).assignmentPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentPk);
    }

    @Override
    public String toString() {
        return "IncidentAssignment{" + assignmentCode + ", team " + teamCode
             + " (" + role + "), " + assignmentStatus + "}";
    }
}
