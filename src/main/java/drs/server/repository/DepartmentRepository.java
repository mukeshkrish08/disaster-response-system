package drs.server.repository;

import drs.shared.enums.DepartmentType;
import drs.shared.exception.DataAccessException;
import drs.shared.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code departments} table.
  
 */
public class DepartmentRepository {

    public Optional<Department> findByPk(int departmentPk) {
        String sql = "SELECT * FROM departments WHERE department_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, departmentPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(department) failed", e);
        }
    }

    public Optional<Department> findByCode(String code) {
        String sql = "SELECT * FROM departments WHERE department_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(department) failed", e);
        }
    }

    public List<Department> findAll() {
        String sql = "SELECT * FROM departments ORDER BY name";
        List<Department> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findAll(department) failed", e);
        }
    }

    public List<Department> findByType(DepartmentType type) {
        String sql = "SELECT * FROM departments WHERE department_type = ?";
        List<Department> result = new ArrayList<>();
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
            throw new DataAccessException("findByType(department) failed", e);
        }
    }

    /*   * Insert a new department row and return the generated primary key.
     * Used by the admin Add Department feature.
     */
    public int save(Department d) {
        String sql = "INSERT INTO departments "
                + "(department_code, name, department_type, active, "
                + " created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getDepartmentCode());
            ps.setString(2, d.getName());
            ps.setString(3, d.getDepartmentType().name());
            ps.setBoolean(4, d.isActive());
            ps.setTimestamp(5, Timestamp.valueOf(
                    d.getCreatedAt() == null
                            ? java.time.LocalDateTime.now()
                            : d.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    d.setDepartmentPk(pk);
                    return pk;
                }
                throw new DataAccessException(
                        "save(department) failed: no generated key");
            }
        } catch (Exception e) {
            throw new DataAccessException("save(department) failed", e);
        }
    }

    /*   * Flip the {@code active} flag on a department. Used by the admin
     * deactivate-department feature.
     */
    public void setActive(int departmentPk, boolean active) {
        String sql = "UPDATE departments SET active = ? "
                + "WHERE department_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, departmentPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("setActive(department) failed", e);
        }
    }

    private Department mapRow(ResultSet rs) throws Exception {
        Department d = new Department();
        d.setDepartmentPk(rs.getInt("department_pk"));
        d.setDepartmentCode(rs.getString("department_code"));
        d.setName(rs.getString("name"));
        d.setDepartmentType(DepartmentType.valueOf(rs.getString("department_type")));
        d.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        d.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return d;
    }
}
