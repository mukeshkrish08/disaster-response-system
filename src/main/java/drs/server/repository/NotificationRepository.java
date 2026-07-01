package drs.server.repository;

import drs.shared.enums.NotificationStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.model.Notification;

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
 * JDBC access to the {@code notifications} table.
  
 */
public class NotificationRepository {

    private static final String SELECT_BASE =
            "SELECT n.*, i.incident_code AS inc_code, "
                    + "u.user_code AS recip_code "
                    + "FROM notifications n "
                    + "LEFT JOIN incidents i ON i.incident_pk = n.incident_pk "
                    + "LEFT JOIN users u ON u.user_pk = n.recipient_user_pk ";

    public int save(Connection c, Notification n) {
        String sql = "INSERT INTO notifications ("
                + "notification_code, incident_pk, recipient_user_pk, "
                + "message, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getNotificationCode());
            ps.setInt(2, n.getIncidentPk());
            ps.setInt(3, n.getRecipientUserPk());
            ps.setString(4, n.getMessage());
            ps.setString(5, n.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(
                    n.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : n.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    n.setNotificationPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(notification) failed", e);
        }
    }

    public Optional<Notification> findByCode(String code) {
        String sql = SELECT_BASE + "WHERE n.notification_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(notification) failed", e);
        }
    }

    public List<Notification> findByRecipientPk(int recipientUserPk) {
        String sql = SELECT_BASE + "WHERE n.recipient_user_pk = ? "
                + "ORDER BY n.created_at DESC";
        List<Notification> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recipientUserPk);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByRecipientPk failed", e);
        }
    }

    public void updateStatus(int notificationPk, NotificationStatus status,
                             LocalDateTime when) {
        String sql = "UPDATE notifications SET status = ?, "
                + "acknowledged_at = ? WHERE notification_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, when == null ? null : Timestamp.valueOf(when));
            ps.setInt(3, notificationPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updateStatus(notification) failed", e);
        }
    }

    private Notification mapRow(ResultSet rs) throws Exception {
        Notification n = new Notification();
        n.setNotificationPk(rs.getInt("notification_pk"));
        n.setNotificationCode(rs.getString("notification_code"));
        n.setIncidentPk(rs.getInt("incident_pk"));
        n.setIncidentCode(rs.getString("inc_code"));
        n.setRecipientUserPk(rs.getInt("recipient_user_pk"));
        n.setRecipientUserCode(rs.getString("recip_code"));
        n.setMessage(rs.getString("message"));
        n.setStatus(NotificationStatus.valueOf(rs.getString("status")));
        Timestamp created = rs.getTimestamp("created_at");
        n.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp ack = rs.getTimestamp("acknowledged_at");
        n.setAcknowledgedAt(ack == null ? null : ack.toLocalDateTime());
        return n;
    }
}
