package drs.shared.enums;

/**
 * Operational status of a resource.
  
 */
public enum ResourceStatus {
    AVAILABLE("Available"),
    ALLOCATED("Allocated"),
    IN_TRANSIT("In transit"),
    MAINTENANCE("Maintenance"),
    RETIRED("Retired");

    private final String displayName;

    ResourceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
