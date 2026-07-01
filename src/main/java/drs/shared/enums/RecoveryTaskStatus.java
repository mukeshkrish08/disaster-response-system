package drs.shared.enums;

/**
 * Lifecycle status of a recovery task.
 * Valid transitions: OPEN -> ASSIGNED -> IN_PROGRESS -> COMPLETED.
 * BLOCKED and CANCELLED are side states reachable from any active state.
  
 */
public enum RecoveryTaskStatus {
    OPEN("Open"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In progress"),
    BLOCKED("Blocked"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    RecoveryTaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
