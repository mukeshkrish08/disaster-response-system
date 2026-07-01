package drs.shared.enums;

/**
 * CAP urgency level. Higher weight = needs response sooner.
  
 */
public enum CapUrgency {
    IMMEDIATE(10, "Immediate"),
    EXPECTED(7,   "Expected"),
    FUTURE(4,     "Future"),
    PAST(1,       "Past"),
    UNKNOWN(1,    "Unknown");

    private final int weight;
    private final String displayName;

    CapUrgency(int weight, String displayName) {
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
