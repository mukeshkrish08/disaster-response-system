package drs.shared.model;

import drs.shared.enums.DamageLevel;
import drs.shared.enums.InfrastructureStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A coordinator or team leader's assessment of damage at an incident
 * site. The {@code notes} field is plaintext in Java but stored
 * AES-encrypted in the {@code notes_encrypted} column.
  
 */
public class DamageAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int assessmentPk;
    private String assessmentCode;
    private int incidentPk;
    private String incidentCode;             // hydrated for display
    private int assessedByUserPk;
    private String assessedByUserCode;       // hydrated for display
    private DamageLevel buildingDamageLevel;
    private InfrastructureStatus roadStatus;
    private InfrastructureStatus powerStatus;
    private InfrastructureStatus waterStatus;
    private int casualtyEstimate;
    private String notes;                    // plaintext in Java
    private LocalDateTime assessedAt;

    public DamageAssessment() {
        // No-arg for JDBC/Serialization
        this.buildingDamageLevel = DamageLevel.NONE;
        this.roadStatus = InfrastructureStatus.OPERATIONAL;
        this.powerStatus = InfrastructureStatus.OPERATIONAL;
        this.waterStatus = InfrastructureStatus.OPERATIONAL;
    }

    public int getAssessmentPk() { return assessmentPk; }
    public void setAssessmentPk(int assessmentPk) { this.assessmentPk = assessmentPk; }

    public String getAssessmentCode() { return assessmentCode; }
    public void setAssessmentCode(String assessmentCode) { this.assessmentCode = assessmentCode; }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public int getAssessedByUserPk() { return assessedByUserPk; }
    public void setAssessedByUserPk(int assessedByUserPk) { this.assessedByUserPk = assessedByUserPk; }

    public String getAssessedByUserCode() { return assessedByUserCode; }
    public void setAssessedByUserCode(String assessedByUserCode) { this.assessedByUserCode = assessedByUserCode; }

    public DamageLevel getBuildingDamageLevel() { return buildingDamageLevel; }
    public void setBuildingDamageLevel(DamageLevel buildingDamageLevel) { this.buildingDamageLevel = buildingDamageLevel; }

    public InfrastructureStatus getRoadStatus() { return roadStatus; }
    public void setRoadStatus(InfrastructureStatus roadStatus) { this.roadStatus = roadStatus; }

    public InfrastructureStatus getPowerStatus() { return powerStatus; }
    public void setPowerStatus(InfrastructureStatus powerStatus) { this.powerStatus = powerStatus; }

    public InfrastructureStatus getWaterStatus() { return waterStatus; }
    public void setWaterStatus(InfrastructureStatus waterStatus) { this.waterStatus = waterStatus; }

    public int getCasualtyEstimate() { return casualtyEstimate; }
    public void setCasualtyEstimate(int casualtyEstimate) { this.casualtyEstimate = casualtyEstimate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DamageAssessment)) return false;
        return assessmentPk == ((DamageAssessment) o).assessmentPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assessmentPk);
    }

    @Override
    public String toString() {
        return "DamageAssessment{" + assessmentCode
             + ", building=" + buildingDamageLevel
             + ", casualties=" + casualtyEstimate + "}";
    }
}
