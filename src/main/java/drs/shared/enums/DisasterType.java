package drs.shared.enums;

/**
 * Categories of disasters that can be reported. These align with the
 * case study's "hurricane, fire, earthquake, flood, etc." plus modern
 * additions.
  
 */
public enum DisasterType {
    FIRE("Fire"),
    BUSHFIRE("Bushfire"),
    FLOOD("Flood"),
    EARTHQUAKE("Earthquake"),
    CYCLONE("Cyclone"),
    HURRICANE("Hurricane"),
    TORNADO("Tornado"),
    STORM("Storm"),
    LANDSLIDE("Landslide"),
    EXPLOSION("Explosion"),
    HAZMAT("Hazardous Material"),
    TSUNAMI("Tsunami"),
    INFRASTRUCTURE_FAILURE("Infrastructure Failure"),
    MEDICAL_EMERGENCY("Medical Emergency");

    private final String displayName;

    DisasterType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static DisasterType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("DisasterType value cannot be null");
        }
        return DisasterType.valueOf(value.trim().toUpperCase().replace(' ', '_'));
    }
}
