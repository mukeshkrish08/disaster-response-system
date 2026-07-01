package drs.server.service;

import drs.server.repository.ResponseTeamRepository;
import drs.shared.enums.DisasterType;
import drs.shared.exception.DispatchException;
import drs.shared.model.Incident;
import drs.shared.model.ResponseTeam;
import drs.shared.util.HaversineCalculator;

import java.util.List;
import java.util.Optional;

/**
 * Suggests primary and secondary response teams for an incident based
 * on disaster type + great-circle distance.
 *
 * Carries A2's Creative Feature 2 forward to A3.
  
 */
public class GeoDispatchService {

    private final ResponseTeamRepository teamRepository;

    public GeoDispatchService(ResponseTeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /*   * Suggest the nearest AVAILABLE team whose department type matches
     * the disaster.
         * @param incident the incident
     * @return nearest matching team
     * @throws DispatchException if no team can be found
     */
    public ResponseTeam suggestPrimaryTeam(Incident incident) {
        String departmentType = getPrimaryDepartmentType(incident.getDisasterType());
        List<ResponseTeam> candidates = teamRepository
                .findAvailableByDepartmentType(departmentType);
        if (candidates.isEmpty()) {
            throw new DispatchException(
                    "No available teams of type " + departmentType
                            + " - try widening the search.");
        }
        return findNearest(candidates, incident.getIncidentLat(),
                incident.getIncidentLon());
    }

    /*   * Suggest a secondary team from a DIFFERENT department type than the
     * primary one (e.g. Hospital backing up Fire).
         * @param incident          the incident
     * @param primaryDeptType   department type of the primary team
     * @return Optional secondary team
     */
    public Optional<ResponseTeam> suggestSecondaryTeam(Incident incident,
                                                       String primaryDeptType) {
        String secondaryDeptType = getSecondaryDepartmentType(
                incident.getDisasterType(), primaryDeptType);
        if (secondaryDeptType == null) {
            return Optional.empty();
        }
        List<ResponseTeam> candidates = teamRepository
                .findAvailableByDepartmentType(secondaryDeptType);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(findNearest(candidates, incident.getIncidentLat(),
                incident.getIncidentLon()));
    }

    /*   * Return the team in {@code candidates} with the smallest Haversine
     * distance to the given coordinates.
         * @param candidates list (must be non-empty)
     * @param lat        target latitude
     * @param lon        target longitude
     * @return nearest team
     */
    public ResponseTeam findNearest(List<ResponseTeam> candidates,
                                    double lat, double lon) {
        ResponseTeam best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ResponseTeam t : candidates) {
            double d = HaversineCalculator.calculateDistance(
                    lat, lon, t.getLatitude(), t.getLongitude());
            if (d < bestDistance) {
                bestDistance = d;
                best = t;
            }
        }
        return best;
    }

    /*   * Convenience method exposing the Haversine distance calculation.
     */
    public double calculateDistance(double lat1, double lon1,
                                    double lat2, double lon2) {
        return HaversineCalculator.calculateDistance(lat1, lon1, lat2, lon2);
    }

    /*   * Map disaster type to the most appropriate primary department.
     */
    private String getPrimaryDepartmentType(DisasterType type) {
        if (type == null) {
            return "POLICE";
        }
        switch (type) {
            case FIRE:
            case BUSHFIRE:
            case EXPLOSION:
                return "FIRE";
            case MEDICAL_EMERGENCY:
                return "HOSPITAL";
            case HAZMAT:
                return "FIRE";
            case FLOOD:
            case CYCLONE:
            case HURRICANE:
            case STORM:
            case TORNADO:
            case TSUNAMI:
            case LANDSLIDE:
            case EARTHQUAKE:
                return "FIRE";
            case INFRASTRUCTURE_FAILURE:
                return "UTILITY";
            default:
                return "POLICE";
        }
    }

    /*   * Map disaster type to a sensible secondary department (different
     * from primary).
     */
    private String getSecondaryDepartmentType(DisasterType type,
                                              String primaryDeptType) {
        if ("HOSPITAL".equals(primaryDeptType)) {
            return "POLICE";
        }
        // Mass-casualty disasters get a Hospital secondary
        if (type != null) {
            switch (type) {
                case EARTHQUAKE:
                case TSUNAMI:
                case HURRICANE:
                case CYCLONE:
                case EXPLOSION:
                case MEDICAL_EMERGENCY:
                case FIRE:
                case BUSHFIRE:
                case HAZMAT:
                    return "HOSPITAL";
                case INFRASTRUCTURE_FAILURE:
                    return "TRANSPORT";
                default:
                    return "POLICE";
            }
        }
        return "POLICE";
    }
}
