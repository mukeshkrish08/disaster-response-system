package drs.shared.model;

import drs.shared.enums.RecoveryTaskStatus;
import drs.shared.enums.RecoveryTaskType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A discrete recovery task assigned to a department (and optionally a
 * specific user) for an incident in the recovery phase. Has its own
 * state machine enforced by RecoveryTaskService.
 *
 * Valid transitions:
 *  OPEN -> ASSIGNED -> IN_PROGRESS -> COMPLETED
 *  any active state -> BLOCKED (with reason)
 *  any active state -> CANCELLED
  
 */
public class RecoveryTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private int taskPk;
    private String taskCode;
    private int incidentPk;
    private String incidentCode;             // hydrated for display
    private int departmentPk;
    private String departmentCode;           // hydrated for display
    private RecoveryTaskType taskType;
    private String description;
    private RecoveryTaskStatus status;
    private Integer assignedToUserPk;        // nullable when OPEN
    private String assignedToUserCode;       // hydrated for display
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String blockedReason;
    private LocalDateTime createdAt;

    public RecoveryTask() {
        // No-arg for JDBC/Serialization
        this.status = RecoveryTaskStatus.OPEN;
    }

    public int getTaskPk() { return taskPk; }
    public void setTaskPk(int taskPk) { this.taskPk = taskPk; }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public int getDepartmentPk() { return departmentPk; }
    public void setDepartmentPk(int departmentPk) { this.departmentPk = departmentPk; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public RecoveryTaskType getTaskType() { return taskType; }
    public void setTaskType(RecoveryTaskType taskType) { this.taskType = taskType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RecoveryTaskStatus getStatus() { return status; }
    public void setStatus(RecoveryTaskStatus status) { this.status = status; }

    public Integer getAssignedToUserPk() { return assignedToUserPk; }
    public void setAssignedToUserPk(Integer assignedToUserPk) { this.assignedToUserPk = assignedToUserPk; }

    public String getAssignedToUserCode() { return assignedToUserCode; }
    public void setAssignedToUserCode(String assignedToUserCode) { this.assignedToUserCode = assignedToUserCode; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecoveryTask)) return false;
        return taskPk == ((RecoveryTask) o).taskPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskPk);
    }

    @Override
    public String toString() {
        return "RecoveryTask{" + taskCode + ", " + taskType + ", " + status + "}";
    }
}
