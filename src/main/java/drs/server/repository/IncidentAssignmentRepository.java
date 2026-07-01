package drs.server.repository;

import drs.shared.enums.AssignmentRole;
import drs.shared.enums.AssignmentStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.model.IncidentAssignment;

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
 * JDBC access to the {@code incident_assignments} table.
  
 */
public class IncidentAssignmentRepository {

    private static final String SELECT_BASE =
            "SELECT a.*, i.incident_code AS inc_code, "
                    + "t.team_code AS tm_code, t.team_name AS tm_name, "
                    + "u.user_code AS asn_user_code "
                    + "FROM incident_assignments a "
                    + "LEFT JOIN incidents i ON i.incident_pk = a.incident_pk "
                    + "LEFT JOIN response_teams t ON t.team_pk = a.team_pk "
                    + "LEFT JOIN users u ON u.user_pk = a.assigned_by_user_pk ";

    /** Save using the caller-supplied connection (supports transactions). */
    public int save(Connection c, IncidentAssignment a) {
        String sql = "INSERT INTO incident_assignments ("
                + "assignment_code, incident_pk, team_pk, role, "
                + "assignment_status, assigned_by_user_pk, distance_km, "
                + "assigned_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getAssignmentCode());
            ps.setInt(2, a.getIncidentPk());
            ps.setInt(3, a.getTeamPk());
            ps.setString(4, a.getRole().name());
            ps.setString(5, a.getAssignmentStatus().name());
            ps.setInt(6, a.getAssignedByUserPk());
            ps.setDouble(7, a.getDistanceKm());
            ps.setTimestamp(8, Timestamp.valueOf(
                    a.getAssignedAt() == null
                            ? LocalDateTime.now()
                            : a.getAssignedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    a.setAssignmentPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(assignment) failed", e);
        }
    }

    public List<IncidentAssignment> findByIncidentPk(int incidentPk) {
        String sql = SELECT_BASE + "WHERE a.incident_pk = ? ORDER BY a.assigned_at";
        List<IncidentAssignment> result = new ArrayList<>();
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
            throw new DataAccessException("findByIncidentPk(assn) failed", e);
        }
    }

    public List<IncidentAssignment> findByLeaderUserPk(int leaderUserPk) {
        String sql = SELECT_BASE
                + "JOIN response_teams t2 ON t2.team_pk = a.team_pk "
                + "WHERE t2.leader_user_pk = ? "
                + "ORDER BY a.assigned_at DESC";
        List<IncidentAssignment> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, leaderUserPk);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByLeaderUserPk(assn) failed", e);
        }
    }

    public void updateStatus(int assignmentPk, AssignmentStatus status,
                             LocalDateTime when) {
        String column = (status == AssignmentStatus.ACTIVE)
                ? "started_at"
                : (status == AssignmentStatus.COMPLETED ? "completed_at" : null);
        String sql = (column == null)
                ? "UPDATE incident_assignments SET assignment_status = ? "
                        + "WHERE assignment_pk = ?"
                : "UPDATE incident_assignments SET assignment_status = ?, "
                        + column + " = ? WHERE assignment_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (column == null) {
                ps.setInt(2, assignmentPk);
            } else {
                ps.setTimestamp(2, Timestamp.valueOf(when));
                ps.setInt(3, assignmentPk);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updateStatus(assn) failed", e);
        }
    }

    private IncidentAssignment mapRow(ResultSet rs) throws Exception {
        IncidentAssignment a = new IncidentAssignment();
        a.setAssignmentPk(rs.getInt("assignment_pk"));
        a.setAssignmentCode(rs.getString("assignment_code"));
        a.setIncidentPk(rs.getInt("incident_pk"));
        a.setIncidentCode(rs.getString("inc_code"));
        a.setTeamPk(rs.getInt("team_pk"));
        a.setTeamCode(rs.getString("tm_code"));
        a.setTeamName(rs.getString("tm_name"));
        a.setRole(AssignmentRole.valueOf(rs.getString("role")));
        a.setAssignmentStatus(AssignmentStatus.valueOf(rs.getString("assignment_status")));
        a.setAssignedByUserPk(rs.getInt("assigned_by_user_pk"));
        a.setAssignedByUserCode(rs.getString("asn_user_code"));
        a.setDistanceKm(rs.getDouble("distance_km"));
        a.setAssignedAt(ts(rs.getTimestamp("assigned_at")));
        a.setStartedAt(ts(rs.getTimestamp("started_at")));
        a.setCompletedAt(ts(rs.getTimestamp("completed_at")));
        return a;
    }

    private static LocalDateTime ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
