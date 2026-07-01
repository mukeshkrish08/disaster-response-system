package drs.server.repository;

import drs.shared.enums.DamageLevel;
import drs.shared.enums.InfrastructureStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.model.DamageAssessment;
import drs.shared.util.AesEncryption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC access to the {@code damage_assessments} table. The
 * {@code notes} field is AES-encrypted on write and decrypted on read.
  
 */
public class DamageAssessmentRepository {

    private static final String SELECT_BASE =
            "SELECT da.*, i.incident_code AS inc_code, "
                    + "u.user_code AS assessor_code "
                    + "FROM damage_assessments da "
                    + "LEFT JOIN incidents i ON i.incident_pk = da.incident_pk "
                    + "LEFT JOIN users u ON u.user_pk = da.assessed_by_user_pk ";

    public int save(DamageAssessment a) {
        String sql = "INSERT INTO damage_assessments ("
                + "assessment_code, incident_pk, assessed_by_user_pk, "
                + "building_damage_level, road_status, power_status, "
                + "water_status, casualty_estimate, notes_encrypted, "
                + "assessed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getAssessmentCode());
            ps.setInt(2, a.getIncidentPk());
            ps.setInt(3, a.getAssessedByUserPk());
            ps.setString(4, a.getBuildingDamageLevel().name());
            ps.setString(5, a.getRoadStatus().name());
            ps.setString(6, a.getPowerStatus().name());
            ps.setString(7, a.getWaterStatus().name());
            ps.setInt(8, a.getCasualtyEstimate());
            ps.setString(9, a.getNotes() == null
                    ? null : AesEncryption.encrypt(a.getNotes()));
            ps.setTimestamp(10, Timestamp.valueOf(
                    a.getAssessedAt() == null
                            ? LocalDateTime.now()
                            : a.getAssessedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int pk = keys.getInt(1);
                    a.setAssessmentPk(pk);
                    return pk;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new DataAccessException("save(damage assessment) failed", e);
        }
    }

    public List<DamageAssessment> findByIncidentPk(int incidentPk) {
        String sql = SELECT_BASE + "WHERE da.incident_pk = ? "
                + "ORDER BY da.assessed_at DESC";
        List<DamageAssessment> result = new ArrayList<>();
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
                    "findByIncidentPk(assessment) failed", e);
        }
    }

    private DamageAssessment mapRow(ResultSet rs) throws Exception {
        DamageAssessment a = new DamageAssessment();
        a.setAssessmentPk(rs.getInt("assessment_pk"));
        a.setAssessmentCode(rs.getString("assessment_code"));
        a.setIncidentPk(rs.getInt("incident_pk"));
        a.setIncidentCode(rs.getString("inc_code"));
        a.setAssessedByUserPk(rs.getInt("assessed_by_user_pk"));
        a.setAssessedByUserCode(rs.getString("assessor_code"));
        a.setBuildingDamageLevel(DamageLevel.valueOf(rs.getString("building_damage_level")));
        a.setRoadStatus(InfrastructureStatus.valueOf(rs.getString("road_status")));
        a.setPowerStatus(InfrastructureStatus.valueOf(rs.getString("power_status")));
        a.setWaterStatus(InfrastructureStatus.valueOf(rs.getString("water_status")));
        a.setCasualtyEstimate(rs.getInt("casualty_estimate"));
        String enc = rs.getString("notes_encrypted");
        try {
            a.setNotes(enc == null ? null : AesEncryption.decrypt(enc));
        } catch (drs.shared.exception.DrsException ex) {
            a.setNotes(enc);
        }
        Timestamp ts = rs.getTimestamp("assessed_at");
        a.setAssessedAt(ts == null ? null : ts.toLocalDateTime());
        return a;
    }
}
