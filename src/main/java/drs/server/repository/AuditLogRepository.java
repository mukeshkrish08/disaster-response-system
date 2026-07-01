package drs.server.repository;

import drs.shared.exception.DataAccessException;
import drs.shared.model.AuditLog;
import drs.shared.util.AesEncryption;

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
 * JDBC access to the {@code audit_logs} table. The {@code details}
 * field is AES-encrypted before write and decrypted on read. The
 * prev_hash / current_hash columns are stored verbatim.
  
 */
public class AuditLogRepository {

    public int save(AuditLog entry) {
        String sql = "INSERT INTO audit_logs ("
                + "audit_code, user_pk, action, entity_type, entity_code, "
                + "details_encrypted, client_ip, success, "
                + "prev_hash, current_hash, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getAuditCode());
            if (entry.getUserPk() == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, entry.getUserPk());
            }
            ps.setString(3, entry.getAction());
            ps.setString(4, entry.getEntityType());
            ps.setString(5, entry.getEntityCode());
            ps.setString(6, entry.getDetails() == null
                    ? null : AesEncryption.encrypt(entry.getDetails()));
            ps.setString(7, entry.getClientIp());
            ps.setBoolean(8, entry.isSuccess());
            ps.setString(9, entry.getPrevHash());
            ps.setString(10, entry.getCurrentHash());
            ps.setTimestamp(11, Timestamp.valueOf(
                    entry.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : entry.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    entry.setAuditPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(audit) failed", e);
        }
    }

    public Optional<AuditLog> findLatest() {
        String sql = "SELECT al.*, u.user_code AS uc "
                + "FROM audit_logs al "
                + "LEFT JOIN users u ON u.user_pk = al.user_pk "
                + "ORDER BY al.audit_pk DESC LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        } catch (Exception e) {
            throw new DataAccessException("findLatest(audit) failed", e);
        }
    }

    public List<AuditLog> findAll() {
        String sql = "SELECT al.*, u.user_code AS uc "
                + "FROM audit_logs al "
                + "LEFT JOIN users u ON u.user_pk = al.user_pk "
                + "ORDER BY al.audit_pk";
        List<AuditLog> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findAll(audit) failed", e);
        }
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT al.*, u.user_code AS uc "
                + "FROM audit_logs al "
                + "LEFT JOIN users u ON u.user_pk = al.user_pk "
                + "ORDER BY al.audit_pk DESC LIMIT ?";
        List<AuditLog> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findRecent(audit) failed", e);
        }
    }

    private AuditLog mapRow(ResultSet rs) throws Exception {
        AuditLog a = new AuditLog();
        a.setAuditPk(rs.getInt("audit_pk"));
        a.setAuditCode(rs.getString("audit_code"));
        int userPk = rs.getInt("user_pk");
        a.setUserPk(rs.wasNull() ? null : userPk);
        a.setUserCode(rs.getString("uc"));
        a.setAction(rs.getString("action"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityCode(rs.getString("entity_code"));
        String enc = rs.getString("details_encrypted");
        try {
            a.setDetails(enc == null ? null : AesEncryption.decrypt(enc));
        } catch (drs.shared.exception.DrsException ex) {
            a.setDetails(enc);
        }
        a.setClientIp(rs.getString("client_ip"));
        a.setSuccess(rs.getBoolean("success"));
        a.setPrevHash(rs.getString("prev_hash"));
        a.setCurrentHash(rs.getString("current_hash"));
        Timestamp created = rs.getTimestamp("created_at");
        a.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return a;
    }
}
