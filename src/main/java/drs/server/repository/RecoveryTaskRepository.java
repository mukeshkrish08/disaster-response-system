package drs.server.repository;

import drs.shared.enums.RecoveryTaskStatus;
import drs.shared.enums.RecoveryTaskType;
import drs.shared.exception.DataAccessException;
import drs.shared.model.RecoveryTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code recovery_tasks} table.
  
 */
public class RecoveryTaskRepository {

    private static final String SELECT_BASE =
            "SELECT rt.*, i.incident_code AS inc_code, "
                    + "d.department_code AS dept_code, "
                    + "u.user_code AS assignee_code "
                    + "FROM recovery_tasks rt "
                    + "LEFT JOIN incidents i ON i.incident_pk = rt.incident_pk "
                    + "LEFT JOIN departments d ON d.department_pk = rt.department_pk "
                    + "LEFT JOIN users u ON u.user_pk = rt.assigned_to_user_pk ";

    public int save(RecoveryTask t) {
        String sql = "INSERT INTO recovery_tasks ("
                + "task_code, incident_pk, department_pk, task_type, "
                + "description, status, assigned_to_user_pk, assigned_at, "
                + "started_at, completed_at, blocked_reason, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTaskCode());
            ps.setInt(2, t.getIncidentPk());
            ps.setInt(3, t.getDepartmentPk());
            ps.setString(4, t.getTaskType().name());
            ps.setString(5, t.getDescription());
            ps.setString(6, t.getStatus().name());
            if (t.getAssignedToUserPk() == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, t.getAssignedToUserPk());
            }
            ps.setTimestamp(8, t.getAssignedAt() == null
                    ? null : Timestamp.valueOf(t.getAssignedAt()));
            ps.setTimestamp(9, t.getStartedAt() == null
                    ? null : Timestamp.valueOf(t.getStartedAt()));
            ps.setTimestamp(10, t.getCompletedAt() == null
                    ? null : Timestamp.valueOf(t.getCompletedAt()));
            ps.setString(11, t.getBlockedReason());
            ps.setTimestamp(12, Timestamp.valueOf(
                    t.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : t.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    t.setTaskPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(recovery task) failed", e);
        }
    }

    public Optional<RecoveryTask> findByPk(int taskPk) {
        String sql = SELECT_BASE + "WHERE rt.task_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(task) failed", e);
        }
    }

    public Optional<RecoveryTask> findByCode(String code) {
        String sql = SELECT_BASE + "WHERE rt.task_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(task) failed", e);
        }
    }

    public List<RecoveryTask> findByIncidentPk(int incidentPk) {
        String sql = SELECT_BASE + "WHERE rt.incident_pk = ? "
                + "ORDER BY rt.created_at";
        List<RecoveryTask> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, incidentPk);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByIncidentPk(task) failed", e);
        }
    }

    public List<RecoveryTask> findByAssignedToPk(int userPk) {
        String sql = SELECT_BASE + "WHERE rt.assigned_to_user_pk = ? "
                + "ORDER BY rt.created_at DESC";
        List<RecoveryTask> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userPk);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByAssignedToPk(task) failed", e);
        }
    }

    public void updateStatus(int taskPk, RecoveryTaskStatus status,
                             String blockedReason, Integer assigneeUserPk,
                             LocalDateTime when) {
        String sql = "UPDATE recovery_tasks SET status = ?, "
                + "blocked_reason = ?, "
                + "assigned_to_user_pk = COALESCE(?, assigned_to_user_pk), "
                + "assigned_at = CASE WHEN ? = 'ASSIGNED' AND assigned_at IS NULL THEN ? ELSE assigned_at END, "
                + "started_at  = CASE WHEN ? = 'IN_PROGRESS' AND started_at IS NULL THEN ? ELSE started_at END, "
                + "completed_at = CASE WHEN ? = 'COMPLETED' THEN ? ELSE completed_at END "
                + "WHERE task_pk = ?";
        Timestamp ts = Timestamp.valueOf(when == null ? LocalDateTime.now() : when);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, blockedReason);
            if (assigneeUserPk == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, assigneeUserPk);
            }
            ps.setString(4, status.name());
            ps.setTimestamp(5, ts);
            ps.setString(6, status.name());
            ps.setTimestamp(7, ts);
            ps.setString(8, status.name());
            ps.setTimestamp(9, ts);
            ps.setInt(10, taskPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updateStatus(task) failed", e);
        }
    }

    private RecoveryTask mapRow(ResultSet rs) throws Exception {
        RecoveryTask t = new RecoveryTask();
        t.setTaskPk(rs.getInt("task_pk"));
        t.setTaskCode(rs.getString("task_code"));
        t.setIncidentPk(rs.getInt("incident_pk"));
        t.setIncidentCode(rs.getString("inc_code"));
        t.setDepartmentPk(rs.getInt("department_pk"));
        t.setDepartmentCode(rs.getString("dept_code"));
        t.setTaskType(RecoveryTaskType.valueOf(rs.getString("task_type")));
        t.setDescription(rs.getString("description"));
        t.setStatus(RecoveryTaskStatus.valueOf(rs.getString("status")));
        int au = rs.getInt("assigned_to_user_pk");
        t.setAssignedToUserPk(rs.wasNull() ? null : au);
        t.setAssignedToUserCode(rs.getString("assignee_code"));
        Timestamp asn = rs.getTimestamp("assigned_at");
        t.setAssignedAt(asn == null ? null : asn.toLocalDateTime());
        Timestamp st = rs.getTimestamp("started_at");
        t.setStartedAt(st == null ? null : st.toLocalDateTime());
        Timestamp cp = rs.getTimestamp("completed_at");
        t.setCompletedAt(cp == null ? null : cp.toLocalDateTime());
        t.setBlockedReason(rs.getString("blocked_reason"));
        Timestamp cr = rs.getTimestamp("created_at");
        t.setCreatedAt(cr == null ? null : cr.toLocalDateTime());
        return t;
    }
}
