package drs.server.repository;

import drs.shared.enums.ResourceStatus;
import drs.shared.enums.ResourceType;
import drs.shared.exception.DataAccessException;
import drs.shared.model.Resource;

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
 * JDBC access to the {@code resources} table.
 *
 * Critical method: {@link #decrementAvailable(Connection, int, int)}
 * uses an atomic UPDATE ... WHERE quantity_available &gt;= ? to prevent
 * over-allocation under concurrent server threads.
  
 */
public class ResourceRepository {

    private static final String SELECT_BASE =
            "SELECT r.*, l.display_name AS loc_display "
                    + "FROM resources r "
                    + "LEFT JOIN locations l ON l.location_pk = r.home_location_pk ";

    public Optional<Resource> findByPk(int resourcePk) {
        String sql = SELECT_BASE + "WHERE r.resource_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, resourcePk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(resource) failed", e);
        }
    }

    public Optional<Resource> findByCode(String code) {
        String sql = SELECT_BASE + "WHERE r.resource_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(resource) failed", e);
        }
    }

    public List<Resource> findAll() {
        String sql = SELECT_BASE + "ORDER BY r.resource_type, r.resource_name";
        return query(sql);
    }

    public List<Resource> findByType(ResourceType type) {
        String sql = SELECT_BASE
                + "WHERE r.resource_type = ? ORDER BY r.resource_name";
        List<Resource> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByType(resource) failed", e);
        }
    }

    public int save(Resource r) {
        String sql = "INSERT INTO resources ("
                + "resource_code, resource_name, resource_type, "
                + "quantity_total, quantity_available, home_location_pk, "
                + "status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getResourceCode());
            ps.setString(2, r.getResourceName());
            ps.setString(3, r.getResourceType().name());
            ps.setInt(4, r.getQuantityTotal());
            ps.setInt(5, r.getQuantityAvailable());
            if (r.getHomeLocationPk() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, r.getHomeLocationPk());
            }
            ps.setString(7, r.getStatus().name());
            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(8, Timestamp.valueOf(r.getCreatedAt() == null
                    ? now : r.getCreatedAt()));
            ps.setTimestamp(9, Timestamp.valueOf(r.getUpdatedAt() == null
                    ? now : r.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    r.setResourcePk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(resource) failed", e);
        }
    }

    /*   * Atomically decrement quantity_available if at least {@code amount}
     * is available. The {@code WHERE quantity_available >= ?} clause
     * makes this race-free.
         * @param c        connection (caller-managed for transaction)
     * @param resourcePk resource pk
     * @param amount   amount to decrement
     * @return true if successfully decremented, false if not enough
     *        available (no rows updated)
     */
    public boolean decrementAvailable(Connection c, int resourcePk, int amount) {
        String sql = "UPDATE resources SET "
                + "quantity_available = quantity_available - ?, "
                + "updated_at = ? "
                + "WHERE resource_pk = ? AND quantity_available >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, resourcePk);
            ps.setInt(4, amount);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            throw new DataAccessException("decrementAvailable failed", e);
        }
    }

    /*   * Atomically increment quantity_available (used on allocation return).
     */
    public boolean incrementAvailable(Connection c, int resourcePk, int amount) {
        String sql = "UPDATE resources SET "
                + "quantity_available = LEAST(quantity_available + ?, quantity_total), "
                + "updated_at = ? "
                + "WHERE resource_pk = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, resourcePk);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            throw new DataAccessException("incrementAvailable failed", e);
        }
    }

    /*   * Update a resource's lifecycle status (AVAILABLE, MAINTENANCE,
     * RETIRED). Used by admin/coordinator lifecycle actions. Distinct
     * from quantity-allocation tracking.
     */
    public void updateStatus(int resourcePk,
                              drs.shared.enums.ResourceStatus status) {
        String sql = "UPDATE resources SET status = ?, updated_at = ? "
                + "WHERE resource_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, resourcePk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updateStatus(resource) failed", e);
        }
    }

    private List<Resource> query(String sql) {
        List<Resource> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("resource query failed", e);
        }
    }

    private Resource mapRow(ResultSet rs) throws Exception {
        Resource r = new Resource();
        r.setResourcePk(rs.getInt("resource_pk"));
        r.setResourceCode(rs.getString("resource_code"));
        r.setResourceName(rs.getString("resource_name"));
        r.setResourceType(ResourceType.valueOf(rs.getString("resource_type")));
        r.setQuantityTotal(rs.getInt("quantity_total"));
        r.setQuantityAvailable(rs.getInt("quantity_available"));
        int loc = rs.getInt("home_location_pk");
        r.setHomeLocationPk(rs.wasNull() ? null : loc);
        r.setHomeLocationDisplay(rs.getString("loc_display"));
        r.setStatus(ResourceStatus.valueOf(rs.getString("status")));
        Timestamp created = rs.getTimestamp("created_at");
        r.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        r.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return r;
    }
}
