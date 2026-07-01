package drs.server.repository;

import drs.shared.enums.UserRole;
import drs.shared.exception.DataAccessException;
import drs.shared.model.User;

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
 * JDBC access to the {@code users} table.
  
 */
public class UserRepository {

    public Optional<User> findByPk(int userPk) {
        String sql = "SELECT * FROM users WHERE user_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(user) failed", e);
        }
    }

    public Optional<User> findByCode(String userCode) {
        String sql = "SELECT * FROM users WHERE user_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(user) failed", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByEmail failed", e);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY user_pk";
        List<User> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findAll(user) failed", e);
        }
    }

    public List<User> findByRole(UserRole role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        List<User> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByRole failed", e);
        }
    }

    public int save(User u) {
        String sql = "INSERT INTO users "
                + "(user_code, full_name, email, password_hash, role, "
                + " department_pk, active, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUserCode());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getRole().name());
            if (u.getDepartmentPk() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, u.getDepartmentPk());
            }
            ps.setBoolean(7, u.isActive());
            ps.setTimestamp(8, Timestamp.valueOf(
                    u.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : u.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    u.setUserPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(user) failed", e);
        }
    }

    public void updateLastLogin(int userPk, LocalDateTime when) {
        String sql = "UPDATE users SET last_login_at = ? WHERE user_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(when));
            ps.setInt(2, userPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updateLastLogin failed", e);
        }
    }

    /*   * Flip the {@code active} flag on a user row. Used by the admin
     * deactivate-user feature. The row is not deleted; allocation
     * history and audit log entries that reference this user remain
     * intact.
     */
    public void setActive(int userPk, boolean active) {
        String sql = "UPDATE users SET active = ? WHERE user_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("setActive(user) failed", e);
        }
    }

    private User mapRow(ResultSet rs) throws Exception {
        User u = new User();
        u.setUserPk(rs.getInt("user_pk"));
        u.setUserCode(rs.getString("user_code"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(UserRole.valueOf(rs.getString("role")));
        int dept = rs.getInt("department_pk");
        u.setDepartmentPk(rs.wasNull() ? null : dept);
        u.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        u.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        u.setLastLoginAt(lastLogin == null ? null : lastLogin.toLocalDateTime());
        return u;
    }
}
