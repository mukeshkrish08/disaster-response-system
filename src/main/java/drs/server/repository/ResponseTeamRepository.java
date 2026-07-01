package drs.server.repository;

import drs.shared.enums.TeamAvailability;
import drs.shared.exception.DataAccessException;
import drs.shared.model.ResponseTeam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code response_teams} table. Joins with
 * {@code departments} and {@code users} to hydrate display fields.
  
 */
public class ResponseTeamRepository {

    private static final String SELECT_BASE =
            "SELECT t.*, d.department_code AS dept_code, "
                    + "d.department_type AS dept_type, "
                    + "u.user_code AS leader_code, "
                    + "u.full_name AS leader_name "
                    + "FROM response_teams t "
                    + "LEFT JOIN departments d ON d.department_pk = t.department_pk "
                    + "LEFT JOIN users u ON u.user_pk = t.leader_user_pk ";

    public Optional<ResponseTeam> findByPk(int teamPk) {
        String sql = SELECT_BASE + "WHERE t.team_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(team) failed", e);
        }
    }

    public Optional<ResponseTeam> findByCode(String code) {
        String sql = SELECT_BASE + "WHERE t.team_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(team) failed", e);
        }
    }

    public List<ResponseTeam> findAll() {
        String sql = SELECT_BASE + "ORDER BY t.team_pk";
        return query(sql);
    }

    public List<ResponseTeam> findAvailable() {
        String sql = SELECT_BASE + "WHERE t.availability = 'AVAILABLE' "
                + "AND t.active = TRUE";
        return query(sql);
    }

    public List<ResponseTeam> findAvailableByDepartmentType(String departmentType) {
        String sql = SELECT_BASE + "WHERE t.availability = 'AVAILABLE' "
                + "AND d.department_type = ? AND t.active = TRUE";
        List<ResponseTeam> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, departmentType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findAvailableByType failed", e);
        }
    }

    public List<ResponseTeam> findByLeaderUserPk(int leaderUserPk) {
        String sql = SELECT_BASE + "WHERE t.leader_user_pk = ?";
        List<ResponseTeam> result = new ArrayList<>();
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
            throw new DataAccessException("findByLeaderUserPk failed", e);
        }
    }

    public void updateAvailability(int teamPk, TeamAvailability availability) {
        String sql = "UPDATE response_teams SET availability = ? WHERE team_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, availability.name());
            ps.setInt(2, teamPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updateAvailability failed", e);
        }
    }

    /*   * Insert a new response team and return the generated primary
     * key. Used by the admin Add Team feature.
     */
    public int save(ResponseTeam t) {
        String sql = "INSERT INTO response_teams "
                + "(team_code, team_name, department_pk, availability, "
                + " latitude, longitude, leader_user_pk, active, "
                + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTeamCode());
            ps.setString(2, t.getTeamName());
            ps.setInt(3, t.getDepartmentPk());
            ps.setString(4, t.getAvailability() == null
                    ? TeamAvailability.AVAILABLE.name()
                    : t.getAvailability().name());
            ps.setDouble(5, t.getLatitude());
            ps.setDouble(6, t.getLongitude());
            if (t.getLeaderUserPk() == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, t.getLeaderUserPk());
            }
            ps.setBoolean(8, t.isActive());
            ps.setTimestamp(9, Timestamp.valueOf(
                    t.getCreatedAt() == null
                            ? java.time.LocalDateTime.now()
                            : t.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    t.setTeamPk(pk);
                    return pk;
                }
                throw new DataAccessException(
                        "save(team) failed: no generated key");
            }
        } catch (Exception e) {
            throw new DataAccessException("save(team) failed", e);
        }
    }

    /*   * Flip the {@code active} flag on a team. Used by the admin
     * deactivate-team feature.
     */
    public void setActive(int teamPk, boolean active) {
        String sql = "UPDATE response_teams SET active = ? WHERE team_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, teamPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("setActive(team) failed", e);
        }
    }

    private List<ResponseTeam> query(String sql) {
        List<ResponseTeam> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("team query failed", e);
        }
    }

    private ResponseTeam mapRow(ResultSet rs) throws Exception {
        ResponseTeam t = new ResponseTeam();
        t.setTeamPk(rs.getInt("team_pk"));
        t.setTeamCode(rs.getString("team_code"));
        t.setTeamName(rs.getString("team_name"));
        t.setDepartmentPk(rs.getInt("department_pk"));
        t.setDepartmentCode(rs.getString("dept_code"));
        t.setDepartmentType(rs.getString("dept_type"));
        t.setAvailability(TeamAvailability.valueOf(rs.getString("availability")));
        t.setLatitude(rs.getDouble("latitude"));
        t.setLongitude(rs.getDouble("longitude"));
        int leader = rs.getInt("leader_user_pk");
        t.setLeaderUserPk(rs.wasNull() ? null : leader);
        t.setLeaderUserCode(rs.getString("leader_code"));
        t.setLeaderName(rs.getString("leader_name"));
        t.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        t.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return t;
    }
}
