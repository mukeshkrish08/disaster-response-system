package drs.server.service;

import drs.shared.model.Incident;
import drs.shared.strategy.CapWeightedStrategy;
import drs.shared.strategy.LifeRiskFirstStrategy;
import drs.shared.strategy.PriorityStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Owns the currently-selected priority strategy and computes scores.
 *
 * Demonstrates the Strategy design pattern - clients use this service
 * without knowing which formula is active.
  
 */
public class PriorityService {

    private volatile PriorityStrategy strategy;

    public PriorityService() {
        // Default strategy
        this.strategy = new CapWeightedStrategy();
    }

    /*   * @param incident the incident to score
     * @return score computed by the current strategy
     */
    public int calculateScore(Incident incident) {
        return strategy.calculateScore(incident);
    }

    /*   * Switch the active strategy by short name.
         * @param strategyName "CAP" or "LIFE_RISK"
     */
    public synchronized void switchStrategy(String strategyName) {
        if (strategyName == null) {
            return;
        }
        String normalised = strategyName.trim().toUpperCase();
        if ("LIFE_RISK".equals(normalised)
                || "LIFE-RISK".equals(normalised)
                || "LIFERISK".equals(normalised)) {
            this.strategy = new LifeRiskFirstStrategy();
        } else {
            this.strategy = new CapWeightedStrategy();
        }
    }

    public String getCurrentStrategyName() {
        return strategy.getName();
    }

    /*   * Return a new list sorted by priority descending.
         * @param incidents source list
     * @return new sorted list (input unchanged)
     */
    public List<Incident> sortByPriority(List<Incident> incidents) {
        List<Incident> copy = new ArrayList<>(incidents);
        copy.sort(Comparator.comparingInt(Incident::getPriorityScore).reversed());
        return copy;
    }
}
