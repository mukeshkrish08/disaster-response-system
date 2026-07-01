package drs.shared.enums;

/**
 * Lifecycle status of an inter-agency notification.
  
 */
public enum NotificationStatus {
    PENDING("Pending"),
    ACKNOWLEDGED("Acknowledged"),
    DISMISSED("Dismissed");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
