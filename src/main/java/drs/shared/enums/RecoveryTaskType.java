package drs.shared.enums;

/**
 * Categories of recovery tasks, matched against department capabilities.
  
 */
public enum RecoveryTaskType {
    DEBRIS_REMOVAL("Debris removal"),
    INFRASTRUCTURE_REPAIR("Infrastructure repair"),
    UTILITY_RESTORATION("Utility restoration"),
    BUILDING_INSPECTION("Building inspection"),
    TRANSPORT_RESTORATION("Transport restoration"),
    PUBLIC_HEALTH("Public health");

    private final String displayName;

    RecoveryTaskType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
