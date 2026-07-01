package drs.server.repository;

import drs.shared.exception.DataAccessException;
import drs.shared.model.Location;

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
 * JDBC access to the {@code locations} table.
  
 */
public class LocationRepository {

    public Optional<Location> findByPk(int locationPk) {
        String sql = "SELECT * FROM locations WHERE location_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(location) failed", e);
        }
    }

    public Optional<Location> findByCode(String code) {
        String sql = "SELECT * FROM locations WHERE location_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(location) failed", e);
        }
    }

    /*   * Return all locations (including deactivated ones). Use this for
     * admin views that need to show full inventory.
     */
    public List<Location> findAll() {
        return runListQuery(
                "SELECT * FROM locations ORDER BY display_name");
    }

    /*   * Return only active locations. Use this for end-user dropdowns
     * (e.g. citizen reporting an incident) so deactivated locations
     * are hidden from new selections without breaking history.
     */
    public List<Location> findAllActive() {
        return runListQuery("SELECT * FROM locations WHERE active = TRUE "
                + "ORDER BY display_name");
    }

    private List<Location> runListQuery(String sql) {
        List<Location> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("location query failed", e);
        }
    }

    /*   * Insert a new location row and return the generated primary key.
     * Used by the admin Add Location feature.
     */
    public int save(Location l) {
        String sql = "INSERT INTO locations "
                + "(location_code, postcode, suburb, state, latitude, "
                + " longitude, risk_zone, display_name, active, "
                + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, l.getLocationCode());
            ps.setString(2, l.getPostcode());
            ps.setString(3, l.getSuburb());
            ps.setString(4, l.getState());
            ps.setDouble(5, l.getLatitude());
            ps.setDouble(6, l.getLongitude());
            ps.setString(7, l.getRiskZone());
            ps.setString(8, l.getDisplayName());
            ps.setBoolean(9, l.isActive());
            ps.setTimestamp(10, Timestamp.valueOf(
                    l.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : l.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    l.setLocationPk(pk);
                    return pk;
                }
                throw new DataAccessException(
                        "save(location) failed: no generated key");
            }
        } catch (Exception e) {
            throw new DataAccessException("save(location) failed", e);
        }
    }

    /*   * Flip the {@code active} flag on a location. Incidents that
     * already reference this location remain unchanged.
     */
    public void setActive(int locationPk, boolean active) {
        String sql = "UPDATE locations SET active = ? WHERE location_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, locationPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("setActive(location) failed", e);
        }
    }

    private Location mapRow(ResultSet rs) throws Exception {
        Location l = new Location();
        l.setLocationPk(rs.getInt("location_pk"));
        l.setLocationCode(rs.getString("location_code"));
        l.setPostcode(rs.getString("postcode"));
        l.setSuburb(rs.getString("suburb"));
        l.setState(rs.getString("state"));
        l.setLatitude(rs.getDouble("latitude"));
        l.setLongitude(rs.getDouble("longitude"));
        l.setRiskZone(rs.getString("risk_zone"));
        l.setDisplayName(rs.getString("display_name"));
        l.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        l.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return l;
    }
}
