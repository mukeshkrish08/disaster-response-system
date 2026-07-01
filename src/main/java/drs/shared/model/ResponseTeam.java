package drs.shared.model;

import drs.shared.enums.TeamAvailability;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A field team belonging to a {@link Department}, with a current
 * location and availability status. May have an optional leader user.
  
 */
public class ResponseTeam implements Serializable {

    private static final long serialVersionUID = 1L;

    private int teamPk;
    private String teamCode;
    private String teamName;
    private int departmentPk;
    private String departmentCode;   // hydrated for convenience
    private String departmentType;   // hydrated for convenience
    private TeamAvailability availability;
    private double latitude;
    private double longitude;
    private Integer leaderUserPk;    // nullable
    /** Hydrated for display: full name of the team's leader user.
     * Populated by ResponseTeamRepository.mapRow() via a join to
     * the users table. Null when the team has no leader assigned
     * or when this object came from a context that doesn't hydrate
     * the name. */
    private String leaderName;
    private String leaderUserCode;   // hydrated for convenience (nullable)
    private boolean active;
    private LocalDateTime createdAt;

    public ResponseTeam() {
        // No-arg for JDBC/Serialization
    }

    public int getTeamPk() { return teamPk; }
    public void setTeamPk(int teamPk) { this.teamPk = teamPk; }

    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getDepartmentPk() { return departmentPk; }
    public void setDepartmentPk(int departmentPk) { this.departmentPk = departmentPk; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentType() { return departmentType; }
    public void setDepartmentType(String departmentType) { this.departmentType = departmentType; }

    public TeamAvailability getAvailability() { return availability; }
    public void setAvailability(TeamAvailability availability) { this.availability = availability; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Integer getLeaderUserPk() { return leaderUserPk; }
    public void setLeaderUserPk(Integer leaderUserPk) { this.leaderUserPk = leaderUserPk; }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public String getLeaderUserCode() { return leaderUserCode; }
    public void setLeaderUserCode(String leaderUserCode) { this.leaderUserCode = leaderUserCode; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseTeam)) return false;
        return teamPk == ((ResponseTeam) o).teamPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamPk);
    }

    @Override
    public String toString() {
        return "ResponseTeam{" + teamCode + ", " + teamName + ", " + availability + "}";
    }
}
