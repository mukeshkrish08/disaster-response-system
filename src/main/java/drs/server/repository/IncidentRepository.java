package drs.server.repository;

import drs.shared.enums.CapCertainty;
import drs.shared.enums.CapSeverity;
import drs.shared.enums.CapUrgency;
import drs.shared.enums.DisasterType;
import drs.shared.enums.IncidentStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.model.Incident;
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
 * JDBC access to the {@code incidents} table.
 *
 * The {@code description} field is AES-encrypted before write and
 * decrypted on read by this repository so callers see plaintext.
  
 */
public class IncidentRepository {

    /*   * Base SELECT for incident queries. Includes left joins to
     * resolve the reporter user code, the location's display name,
     * and (when the incident has a current PRIMARY assignment) the
     * assigned team's name plus its leader's full name. The team /
     * leader columns will be NULL for incidents that haven't been
     * assigned yet, which is the expected state for REPORTED and
     * ASSESSED incidents.
     */
    private static final String SELECT_BASE =
            "SELECT i.*, u.user_code AS reporter_code, "
                    + "u.full_name AS reporter_name, "
                    + "l.display_name AS location_display, "
                    + "rt.team_name AS assigned_team_name, "
                    + "lu.full_name AS assigned_leader_name "
                    + "FROM incidents i "
                    + "LEFT JOIN users u ON u.user_pk = i.reported_by_user_pk "
                    + "LEFT JOIN locations l ON l.location_pk = i.location_pk "
                    + "LEFT JOIN incident_assignments ia "
                    + "  ON ia.incident_pk = i.incident_pk "
                    + "  AND ia.role = 'PRIMARY' "
                    + "  AND ia.assignment_status IN ('PENDING','ACTIVE') "
                    + "LEFT JOIN response_teams rt ON rt.team_pk = ia.team_pk "
                    + "LEFT JOIN users lu ON lu.user_pk = rt.leader_user_pk ";

    public Optional<Incident> findByPk(int incidentPk) {
        String sql = SELECT_BASE + "WHERE i.incident_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, incidentPk);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByPk(incident) failed", e);
        }
    }

    public Optional<Incident> findByCode(String code) {
        String sql = SELECT_BASE + "WHERE i.incident_code = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DataAccessException("findByCode(incident) failed", e);
        }
    }

    public List<Incident> findAll() {
        String sql = SELECT_BASE + "ORDER BY i.priority_score DESC, i.reported_at DESC";
        return query(sql);
    }

    public List<Incident> findByReportedBy(int userPk) {
        String sql = SELECT_BASE + "WHERE i.reported_by_user_pk = ? "
                + "ORDER BY i.reported_at DESC";
        List<Incident> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userPk);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("findByReportedBy failed", e);
        }
    }

    public int save(Incident incident) {
        String sql = "INSERT INTO incidents ("
                + "incident_code, reported_by_user_pk, disaster_type, location_pk, "
                + "incident_lat, incident_lon, description_encrypted, contact_phone, "
                + "people_affected, property_risk_level, "
                + "cap_severity, cap_urgency, cap_certainty, priority_score, "
                + "status, reported_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, incident.getIncidentCode());
            ps.setInt(2, incident.getReportedByUserPk());
            ps.setString(3, incident.getDisasterType().name());
            ps.setInt(4, incident.getLocationPk());
            ps.setDouble(5, incident.getIncidentLat());
            ps.setDouble(6, incident.getIncidentLon());
            ps.setString(7, AesEncryption.encrypt(incident.getDescription()));
            ps.setString(8, incident.getContactPhone() == null ? "" : incident.getContactPhone());
            ps.setInt(9, incident.getPeopleAffected());
            ps.setString(10, incident.getPropertyRiskLevel());
            ps.setString(11, incident.getCapSeverity().name());
            ps.setString(12, incident.getCapUrgency().name());
            ps.setString(13, incident.getCapCertainty().name());
            ps.setInt(14, incident.getPriorityScore());
            ps.setString(15, incident.getStatus().name());
            ps.setTimestamp(16, Timestamp.valueOf(
                    incident.getReportedAt() == null
                            ? LocalDateTime.now()
                            : incident.getReportedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    incident.setIncidentPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(incident) failed", e);
        }
    }

    public void update(Incident i) {
        String sql = "UPDATE incidents SET "
                + "cap_severity = ?, cap_urgency = ?, cap_certainty = ?, "
                + "priority_score = ?, status = ?, "
                + "assessed_at = ?, resolved_at = ?, closed_at = ?, closed_by = ? "
                + "WHERE incident_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, i.getCapSeverity().name());
            ps.setString(2, i.getCapUrgency().name());
            ps.setString(3, i.getCapCertainty().name());
            ps.setInt(4, i.getPriorityScore());
            ps.setString(5, i.getStatus().name());
            ps.setTimestamp(6, i.getAssessedAt() == null
                    ? null : Timestamp.valueOf(i.getAssessedAt()));
            ps.setTimestamp(7, i.getResolvedAt() == null
                    ? null : Timestamp.valueOf(i.getResolvedAt()));
            ps.setTimestamp(8, i.getClosedAt() == null
                    ? null : Timestamp.valueOf(i.getClosedAt()));
            ps.setString(9, i.getClosedBy());
            ps.setInt(10, i.getIncidentPk());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("update(incident) failed", e);
        }
    }

    public void updatePriorityScore(int incidentPk, int score) {
        String sql = "UPDATE incidents SET priority_score = ? WHERE incident_pk = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setInt(2, incidentPk);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("updatePriorityScore failed", e);
        }
    }

    private List<Incident> query(String sql) {
        List<Incident> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new DataAccessException("incident query failed", e);
        }
    }

    private Incident mapRow(ResultSet rs) throws Exception {
        Incident i = new Incident();
        i.setIncidentPk(rs.getInt("incident_pk"));
        i.setIncidentCode(rs.getString("incident_code"));
        i.setReportedByUserPk(rs.getInt("reported_by_user_pk"));
        i.setReportedByUserCode(rs.getString("reporter_code"));
        i.setReportedByUserName(rs.getString("reporter_name"));
        i.setDisasterType(DisasterType.valueOf(rs.getString("disaster_type")));
        i.setLocationPk(rs.getInt("location_pk"));
        i.setLocationDisplayName(rs.getString("location_display"));
        // Hydrated team + leader from incident_assignments JOIN. Null
        // is fine - controllers show "Not yet assigned" in that case.
        i.setAssignedTeamName(rs.getString("assigned_team_name"));
        i.setAssignedLeaderName(rs.getString("assigned_leader_name"));
        i.setIncidentLat(rs.getDouble("incident_lat"));
        i.setIncidentLon(rs.getDouble("incident_lon"));
        // Decrypt description; fall back to raw value (e.g. for seed data
        // inserted as plaintext before the AES key existed)
        String enc = rs.getString("description_encrypted");
        try {
            i.setDescription(AesEncryption.decrypt(enc));
        } catch (drs.shared.exception.DrsException ex) {
            i.setDescription(enc);
        }
        i.setContactPhone(rs.getString("contact_phone"));
        i.setPeopleAffected(rs.getInt("people_affected"));
        i.setPropertyRiskLevel(rs.getString("property_risk_level"));
        i.setCapSeverity(CapSeverity.valueOf(rs.getString("cap_severity")));
        i.setCapUrgency(CapUrgency.valueOf(rs.getString("cap_urgency")));
        i.setCapCertainty(CapCertainty.valueOf(rs.getString("cap_certainty")));
        i.setPriorityScore(rs.getInt("priority_score"));
        i.setStatus(IncidentStatus.valueOf(rs.getString("status")));
        i.setReportedAt(ts(rs.getTimestamp("reported_at")));
        i.setAssessedAt(ts(rs.getTimestamp("assessed_at")));
        i.setResolvedAt(ts(rs.getTimestamp("resolved_at")));
        i.setClosedAt(ts(rs.getTimestamp("closed_at")));
        i.setClosedBy(rs.getString("closed_by"));
        return i;
    }

    private static LocalDateTime ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
