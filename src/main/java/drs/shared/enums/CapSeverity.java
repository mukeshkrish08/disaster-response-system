package drs.shared.enums;

/**
 * CAP (Common Alerting Protocol) severity level. Each level carries a
 * numeric weight used by the priority scoring strategies.
  
 */
public enum CapSeverity {
    EXTREME(10, "Extreme"),
    SEVERE(8,  "Severe"),
    MODERATE(5,"Moderate"),
    MINOR(2,   "Minor"),
    UNKNOWN(1, "Unknown");

    private final int weight;
    private final String displayName;

    CapSeverity(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int weight() {
        return weight;
    }

    public String displayName() {
        return displayName;
    }
}
