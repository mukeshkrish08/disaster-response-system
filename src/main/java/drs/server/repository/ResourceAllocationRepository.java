package drs.server.repository;

import drs.shared.exception.DataAccessException;
import drs.shared.model.ResourceAllocation;

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
 * JDBC access to the {@code resource_allocations} table.
  
 */
public class ResourceAllocationRepository {

    private static final String SELECT_BASE =
            "SELECT a.*, r.resource_code AS res_code, "
                    + "r.resource_name AS res_name, i.incident_code AS inc_code, "
                    + "u.user_code AS alloc_user_code "
                    + "FROM resource_allocations a "
                    + "LEFT JOIN resources r ON r.resource_pk = a.resource_pk "
                    + "LEFT JOIN incidents i ON i.incident_pk = a.incident_pk "
                    + "LEFT JOIN users u ON u.user_pk = a.allocated_by_user_pk ";

    public int save(Connection c, ResourceAllocation a) {
        String sql = "INSERT INTO resource_allocations ("
                + "allocation_code, resource_pk, incident_pk, "
                + "quantity_allocated, allocated_by_user_pk, allocated_at, "
                + "notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getAllocationCode());
            ps.setInt(2, a.getResourcePk());
            ps.setInt(3, a.getIncidentPk());
            ps.setInt(4, a.getQuantityAllocated());
            ps.setInt(5, a.getAllocatedByUserPk());
            ps.setTimestamp(6, Timestamp.valueOf(
                    a.getAllocatedAt() == null
                            ? LocalDateTime.now()
                            : a.getAllocatedAt()));
            ps.setString(7, a.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    a.setAllocationPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(allocation) failed", e);
        }
    }

    public Optional<ResourceAllocation> findByPk(int allocationPk) {
        String sql = SELECT_BASE + "WHERE a.allocation_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, allocationPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(allocation) failed", e);
        }
    }

    public Optional<ResourceAllocation> findByCode(String code) {
        String sql = SELECT_BASE + "WHERE a.allocation_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(allocation) failed", e);
        }
    }

    public List<ResourceAllocation> findByIncidentPk(int incidentPk) {
        String sql = SELECT_BASE + "WHERE a.incident_pk = ? "
                + "ORDER BY a.allocated_at DESC";
        List<ResourceAllocation> result = new ArrayList<>();
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
            throw new DataAccessException(
                    "findByIncidentPk(allocation) failed", e);
        }
    }

    public void markReturned(Connection c, int allocationPk, LocalDateTime when) {
        String sql = "UPDATE resource_allocations SET returned_at = ? "
                + "WHERE allocation_pk = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(when));
            ps.setInt(2, allocationPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("markReturned failed", e);
        }
    }

    private ResourceAllocation mapRow(ResultSet rs) throws Exception {
        ResourceAllocation a = new ResourceAllocation();
        a.setAllocationPk(rs.getInt("allocation_pk"));
        a.setAllocationCode(rs.getString("allocation_code"));
        a.setResourcePk(rs.getInt("resource_pk"));
        a.setResourceCode(rs.getString("res_code"));
        a.setResourceName(rs.getString("res_name"));
        a.setIncidentPk(rs.getInt("incident_pk"));
        a.setIncidentCode(rs.getString("inc_code"));
        a.setQuantityAllocated(rs.getInt("quantity_allocated"));
        a.setAllocatedByUserPk(rs.getInt("allocated_by_user_pk"));
        a.setAllocatedByUserCode(rs.getString("alloc_user_code"));
        Timestamp ts = rs.getTimestamp("allocated_at");
        a.setAllocatedAt(ts == null ? null : ts.toLocalDateTime());
        Timestamp rts = rs.getTimestamp("returned_at");
        a.setReturnedAt(rts == null ? null : rts.toLocalDateTime());
        a.setNotes(rs.getString("notes"));
        return a;
    }
}
