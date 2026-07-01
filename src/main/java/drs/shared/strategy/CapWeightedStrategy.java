package drs.shared.strategy;

import drs.shared.enums.DisasterType;
import drs.shared.model.Incident;

/**
 * CAP-weighted priority strategy. Combines CAP severity * CAP urgency
 * with a disaster-type bonus, CAP certainty, and a people-affected bonus.
 *
 * Formula:
 *  score = severity.weight * urgency.weight
 *        + disasterTypeBonus
 *        + certainty.weight
 *        + peopleBonus
  
 */
public class CapWeightedStrategy implements PriorityStrategy {

    @Override
    public int calculateScore(Incident incident) {
        if (incident == null) {
            return 0;
        }
        int severity  = incident.getCapSeverity()  == null ? 1 : incident.getCapSeverity().weight();
        int urgency   = incident.getCapUrgency()   == null ? 1 : incident.getCapUrgency().weight();
        int certainty = incident.getCapCertainty() == null ? 1 : incident.getCapCertainty().weight();
        int people    = incident.getPeopleAffected();

        int base = severity * urgency;
        int typeBonus = getDisasterTypeBonus(incident.getDisasterType());
        int peopleBonus = Math.min(people / 5, 10);  // capped contribution

        return base + typeBonus + certainty + peopleBonus;
    }

    @Override
    public String getName() {
        return "CAP weighted";
    }

    /*   * Bonus for disaster types considered higher-impact by default.
         * @param type the disaster type (nullable)
     * @return bonus to add to score
     */
    private int getDisasterTypeBonus(DisasterType type) {
        if (type == null) {
            return 0;
        }
        switch (type) {
            case TSUNAMI:
            case EARTHQUAKE:
            case HURRICANE:
            case CYCLONE:
            case TORNADO:
                return 8;
            case FIRE:
            case BUSHFIRE:
            case EXPLOSION:
            case HAZMAT:
                return 6;
            case FLOOD:
            case LANDSLIDE:
            case STORM:
                return 4;
            case INFRASTRUCTURE_FAILURE:
            case MEDICAL_EMERGENCY:
                return 2;
            default:
                return 0;
        }
    }
}
