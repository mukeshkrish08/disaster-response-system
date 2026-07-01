package drs.server.util;

import drs.server.repository.DatabaseConnection;
import drs.shared.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates the human-readable code part of every entity ID
 * (e.g. INC-2026-0007).
 *
 * The AUTO_INCREMENT primary key is owned by MySQL. This class owns the
 * readable code that sits alongside it. On server startup,
 * {@link #initialiseFromDatabase()} queries MAX(pk) on each table and
 * sets per-table counters accordingly, so codes survive restarts.
  
 */
public final class IdGenerator {

    /** Counters keyed by entity prefix (USR, INC, etc.). */
    private static final Map<String, AtomicInteger> COUNTERS = new HashMap<>();

    private IdGenerator() {
        // Static utility
    }

    /*   * Initialise per-table counters from MAX(pk) at startup. Call once
     * from server startup, after DatabaseBootstrap has run.
     */
    public static synchronized void initialiseFromDatabase() {
        seed("USR", "users",                "user_pk");
        seed("DEP", "departments",          "department_pk");
        seed("LOC", "locations",            "location_pk");
        seed("TM",  "response_teams",       "team_pk");
        seed("INC", "incidents",            "incident_pk");
        seed("ASG", "incident_assignments", "assignment_pk");
        seed("UPD", "incident_updates",     "update_pk");
        seed("NTF", "notifications",        "notification_pk");
        seed("AUD", "audit_logs",           "audit_pk");
        seed("RES", "resources",            "resource_pk");
        seed("RAL", "resource_allocations", "allocation_pk");
        seed("DAM", "damage_assessments",   "assessment_pk");
        seed("RTK", "recovery_tasks",       "task_pk");
    }

    private static void seed(String prefix, String table, String pkColumn) {
        String sql = "SELECT COALESCE(MAX(" + pkColumn + "), 0) FROM " + table;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int max = rs.next() ? rs.getInt(1) : 0;
            COUNTERS.put(prefix, new AtomicInteger(max));
        } catch (Exception e) {
            // Table might not exist yet during very first run - start at 0
            COUNTERS.put(prefix, new AtomicInteger(0));
        }
    }

    public static String generateUserCode()        { return next("USR"); }
    public static String generateDepartmentCode()  { return next("DEP"); }
    public static String generateLocationCode()    { return next("LOC"); }
    public static String generateTeamCode()        { return next("TM");  }
    public static String generateIncidentCode()    { return next("INC"); }
    public static String generateAssignmentCode()  { return next("ASG"); }
    public static String generateUpdateCode()      { return next("UPD"); }
    public static String generateNotificationCode() { return next("NTF"); }
    public static String generateAuditCode()       { return next("AUD"); }
    public static String generateResourceCode()    { return next("RES"); }
    public static String generateAllocationCode()  { return next("RAL"); }
    public static String generateAssessmentCode()  { return next("DAM"); }
    public static String generateTaskCode()        { return next("RTK"); }

    /*   * Build the next readable code for a prefix in {@code PREFIX-YYYY-####}
     * form.
         * @param prefix entity prefix (USR, INC, etc.)
     * @return readable code
     */
    private static String next(String prefix) {
        AtomicInteger counter = COUNTERS.computeIfAbsent(prefix,
                k -> new AtomicInteger(0));
        int n = counter.incrementAndGet();
        int year = LocalDate.now().getYear();
        return String.format("%s-%d-%04d", prefix, year, n);
    }
}
