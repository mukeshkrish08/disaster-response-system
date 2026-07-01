package drs.shared.enums;

/**
 * Availability state of a response team.
  
 */
public enum TeamAvailability {
    AVAILABLE("Available"),
    BUSY("Busy"),
    OFF_DUTY("Off duty");

    private final String displayName;

    TeamAvailability(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
