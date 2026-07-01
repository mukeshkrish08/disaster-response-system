package drs.shared.enums;

/**
 * Functional category of a department. Used by the geo-dispatch service
 * to match disaster types to suitable response departments.
  
 */
public enum DepartmentType {
    FIRE("Fire & Rescue"),
    HOSPITAL("Hospital"),
    POLICE("Police"),
    UTILITY("Utility"),
    TRANSPORT("Transport"),
    WASTE_MGMT("Waste Management"),
    WATER("Water"),
    SCHOOL("School");

    private final String displayName;

    DepartmentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
