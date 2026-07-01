package drs.shared.protocol;

/**
 * Every server operation the client may invoke. The {@code RequestRouter}
 * dispatches a {@code Request} to the right handler using this enum as the
 * key, providing O(1) dispatch via {@code EnumMap}.
 *
 * Adding a new operation requires:
 *  1. New constant here
 *  2. New handler method in one of the per-feature RequestHandlers
 *  3. New entry registered in RequestRouter
  
 */
public enum OperationType {

    // -- Authentication (AuthRequestHandler) -------------------------------
    LOGIN,
    LOGOUT,
    REGISTER_CITIZEN,

    // -- Incident lifecycle (IncidentRequestHandler) -----------------------
    REPORT_INCIDENT,
    ASSESS_INCIDENT,
    REJECT_INCIDENT,
    /*   * Citizen-initiated withdrawal. Only the original reporter,
     * only while the incident is still in REPORTED state.
     */
    WITHDRAW_INCIDENT,
    CLOSE_INCIDENT,
    LIST_INCIDENTS,
    GET_INCIDENT_DETAILS,
    LIST_MY_REPORTS,
    SWITCH_PRIORITY_STRATEGY,
    GET_CURRENT_STRATEGY,
    GET_INCIDENT_HISTORY,

    // -- Assignment (AssignmentRequestHandler) -----------------------------
    SUGGEST_TEAMS,
    ASSIGN_TEAMS,
    START_RESPONSE,
    COMPLETE_RESPONSE,
    LIST_INCIDENTS_BY_TEAM_LEADER,
    LIST_AVAILABLE_TEAMS,

    // -- Notifications (NotificationRequestHandler) ------------------------
    LIST_NOTIFICATIONS,
    ACKNOWLEDGE_NOTIFICATION,

    // -- Resources (ResourceRequestHandler) - Feature 1 --------------------
    LIST_RESOURCES,
    ADD_RESOURCE,
    ALLOCATE_RESOURCE,
    RETURN_ALLOCATION,
    LIST_ALLOCATIONS_FOR_INCIDENT,
    // Resource lifecycle: move resources between AVAILABLE,
    // MAINTENANCE, and RETIRED states.
    SEND_RESOURCE_TO_MAINTENANCE,
    RETURN_RESOURCE_FROM_MAINTENANCE,
    RETIRE_RESOURCE,

    // -- Damage / Recovery (DamageRecoveryRequestHandler) - Feature 2 ------
    RECORD_DAMAGE_ASSESSMENT,
    LIST_DAMAGE_ASSESSMENTS,
    CREATE_RECOVERY_TASK,
    UPDATE_RECOVERY_TASK_STATUS,
    LIST_RECOVERY_TASKS,
    LIST_MY_RECOVERY_TASKS,

    // -- Admin (AdminRequestHandler) ---------------------------------------
    LIST_USERS,
    LIST_DEPARTMENTS,
    LIST_TEAMS,
    LIST_AUDIT_LOG,
    VERIFY_AUDIT_CHAIN,
    LIST_LOCATIONS,
    // Admin CRUD operations. Staff accounts are admin-provisioned;
    // citizen accounts use REGISTER_CITIZEN (self-signup).
    CREATE_STAFF_USER,
    DEACTIVATE_USER,
    ADD_TEAM,
    DEACTIVATE_TEAM,
    ADD_DEPARTMENT,
    DEACTIVATE_DEPARTMENT,
    ADD_LOCATION,
    DEACTIVATE_LOCATION
}
