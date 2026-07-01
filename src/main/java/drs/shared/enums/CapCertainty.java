package drs.shared.enums;

/**
 * CAP certainty level. Higher weight = more certain the event is real.
  
 */
public enum CapCertainty {
    OBSERVED(10, "Observed"),
    LIKELY(7,    "Likely"),
    POSSIBLE(4,  "Possible"),
    UNLIKELY(1,  "Unlikely"),
    UNKNOWN(1,   "Unknown");

    private final int weight;
    private final String displayName;

    CapCertainty(int weight, String displayName) {
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
