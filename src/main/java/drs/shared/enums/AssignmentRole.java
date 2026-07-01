package drs.shared.enums;

/**
 * Role of a team within an incident assignment.
  
 */
public enum AssignmentRole {
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    SUPPORT("Support");

    private final String displayName;

    AssignmentRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
