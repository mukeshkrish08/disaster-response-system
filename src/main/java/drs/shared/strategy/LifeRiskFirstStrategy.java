package drs.shared.strategy;

import drs.shared.model.Incident;

/**
 * Life-risk-first priority strategy. Weights people affected very heavily
 * relative to CAP fields, useful when the coordinator wants to prioritise
 * mass-casualty events regardless of CAP labels.
 *
 * Formula:
 *  score = peopleAffected * 10 + severity.weight * 5
  
 */
public class LifeRiskFirstStrategy implements PriorityStrategy {

    @Override
    public int calculateScore(Incident incident) {
        if (incident == null) {
            return 0;
        }
        int severity = incident.getCapSeverity() == null
                ? 1
                : incident.getCapSeverity().weight();
        return incident.getPeopleAffected() * 10 + severity * 5;
    }

    @Override
    public String getName() {
        return "Life-risk first";
    }
}
