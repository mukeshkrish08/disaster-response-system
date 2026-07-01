package drs.shared.enums;

/**
 * Lifecycle states of an Incident. Valid transitions are encoded in
 * {@code drs.shared.state.IncidentStateRegistry}.
  
 */
public enum IncidentStatus {
    REPORTED("Reported"),
    ASSESSED("Assessed"),
    ASSIGNED("Assigned"),
    RESPONDING("Responding"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REJECTED("Rejected"),
    /*   * Citizen withdrew their own report before it was assessed.
     * Terminal state, reachable only from REPORTED.
     */
    WITHDRAWN("Withdrawn");

    private final String displayName;

    IncidentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static IncidentStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("IncidentStatus value cannot be null");
        }
        return IncidentStatus.valueOf(value.trim().toUpperCase());
    }
}
