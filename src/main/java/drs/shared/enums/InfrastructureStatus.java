package drs.shared.enums;

/**
 * Operational status of an infrastructure system (road, power, water).
  
 */
public enum InfrastructureStatus {
    OPERATIONAL("Operational"),
    DEGRADED("Degraded"),
    OFFLINE("Offline"),
    DESTROYED("Destroyed");

    private final String displayName;

    InfrastructureStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
