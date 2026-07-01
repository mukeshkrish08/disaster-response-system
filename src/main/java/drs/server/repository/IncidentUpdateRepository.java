package drs.server.repository;

import drs.shared.enums.IncidentStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.model.IncidentUpdate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC access to the {@code incident_updates} table.
  
 */
public class IncidentUpdateRepository {

    private static final String SELECT_BASE =
            "SELECT iu.*, u.user_code AS updater_code "
                    + "FROM incident_updates iu "
                    + "LEFT JOIN users u ON u.user_pk = iu.updated_by_pk ";

    /** Save with a caller-supplied connection (transaction-friendly). */
    public int save(Connection c, IncidentUpdate u) {
        String sql = "INSERT INTO incident_updates ("
                + "update_code, incident_pk, updated_by_pk, "
                + "status_before, status_after, comment, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUpdateCode());
            ps.setInt(2, u.getIncidentPk());
            ps.setInt(3, u.getUpdatedByPk());
            ps.setString(4, u.getStatusBefore().name());
            ps.setString(5, u.getStatusAfter().name());
            ps.setString(6, u.getComment());
            ps.setTimestamp(7, Timestamp.valueOf(
                    u.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : u.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    u.setUpdatePk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(update) failed", e);
        }
    }

    public List<IncidentUpdate> findByIncidentPk(int incidentPk) {
        String sql = SELECT_BASE
                + "WHERE iu.incident_pk = ? ORDER BY iu.created_at";
        List<IncidentUpdate> result = new ArrayList<>();
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
            throw new DataAccessException("findByIncidentPk(update) failed", e);
        }
    }

    private IncidentUpdate mapRow(ResultSet rs) throws Exception {
        IncidentUpdate u = new IncidentUpdate();
        u.setUpdatePk(rs.getInt("update_pk"));
        u.setUpdateCode(rs.getString("update_code"));
        u.setIncidentPk(rs.getInt("incident_pk"));
        u.setUpdatedByPk(rs.getInt("updated_by_pk"));
        u.setUpdatedByUserCode(rs.getString("updater_code"));
        u.setStatusBefore(IncidentStatus.valueOf(rs.getString("status_before")));
        u.setStatusAfter(IncidentStatus.valueOf(rs.getString("status_after")));
        u.setComment(rs.getString("comment"));
        Timestamp t = rs.getTimestamp("created_at");
        u.setCreatedAt(t == null ? null : t.toLocalDateTime());
        return u;
    }
}
