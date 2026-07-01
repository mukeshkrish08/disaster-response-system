package drs.shared.enums;

/**
 * Roles assigned to system users. Determines what operations each user
 * may invoke on the server (see AuthorizationService).
  
 */
public enum UserRole {
    CITIZEN("Citizen"),
    COORDINATOR("Coordinator"),
    TEAM_LEADER("Team Leader"),
    AGENCY_REP("Agency Representative"),
    ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /*   * Parse a role name leniently from a string (case-insensitive,
     * tolerates underscores or spaces).
         * @param value raw input string
     * @return matching UserRole
     * @throws IllegalArgumentException if no match
     */
    public static UserRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("UserRole value cannot be null");
        }
        String normalised = value.trim().toUpperCase().replace(' ', '_');
        return UserRole.valueOf(normalised);
    }
}
