package drs.shared.enums;

/**
 * Severity of damage observed during an incident assessment.
  
 */
public enum DamageLevel {
    NONE("None"),
    MINOR("Minor"),
    MODERATE("Moderate"),
    MAJOR("Major"),
    SEVERE("Severe"),
    CATASTROPHIC("Catastrophic");

    private final String displayName;

    DamageLevel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
