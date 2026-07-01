package drs.shared.strategy;

import drs.shared.model.Incident;

/**
 * Strategy interface for computing the priority score of an incident.
 * The {@code PriorityService} delegates to an implementation chosen at
 * runtime; users may switch strategies via the SWITCH_PRIORITY_STRATEGY
 * operation.
  
 */
public interface PriorityStrategy {

    /*   * Compute the priority score for an incident. Higher is more urgent.
         * @param incident the incident
     * @return integer score (any range)
     */
    int calculateScore(Incident incident);

    /*   * Human-readable strategy name shown in the UI.
         * @return display name
     */
    String getName();
}
