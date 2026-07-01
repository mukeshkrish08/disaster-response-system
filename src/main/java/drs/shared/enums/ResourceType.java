package drs.shared.enums;

/**
 * Categories of physical resources managed by the system.
  
 */
public enum ResourceType {
    VEHICLE("Vehicle"),
    MEDICAL_SUPPLY("Medical Supply"),
    EQUIPMENT("Equipment"),
    FOOD_WATER("Food & Water"),
    SHELTER("Shelter"),
    COMMUNICATION("Communication");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
