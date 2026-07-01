package drs.shared.enums;

/**
 * Lifecycle status of an incident assignment.
  
 */
public enum AssignmentStatus {
    PENDING("Pending"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    AssignmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
