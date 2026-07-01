package drs.server.service;

import drs.shared.enums.CapCertainty;
import drs.shared.enums.CapSeverity;
import drs.shared.enums.CapUrgency;
import drs.shared.enums.DisasterType;
import drs.shared.model.Incident;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PriorityService}.
  
 */
class PriorityServiceTest {

    @Test
    void testDefaultStrategyIsCap() {
        PriorityService ps = new PriorityService();
        assertEquals("CAP weighted", ps.getCurrentStrategyName());
    }

    @Test
    void testSwitchToLifeRisk() {
        PriorityService ps = new PriorityService();
        ps.switchStrategy("LIFE_RISK");
        assertEquals("Life-risk first", ps.getCurrentStrategyName());
    }

    @Test
    void testSortByPriorityDescending() {
        PriorityService ps = new PriorityService();

        Incident low = new Incident();
        low.setDisasterType(DisasterType.STORM);
        low.setCapSeverity(CapSeverity.MINOR);
        low.setCapUrgency(CapUrgency.PAST);
        low.setCapCertainty(CapCertainty.UNLIKELY);
        low.setPriorityScore(ps.calculateScore(low));

        Incident high = new Incident();
        high.setDisasterType(DisasterType.FIRE);
        high.setCapSeverity(CapSeverity.EXTREME);
        high.setCapUrgency(CapUrgency.IMMEDIATE);
        high.setCapCertainty(CapCertainty.OBSERVED);
        high.setPeopleAffected(20);
        high.setPriorityScore(ps.calculateScore(high));

        List<Incident> sorted = ps.sortByPriority(Arrays.asList(low, high));
        assertEquals(high.getPriorityScore(), sorted.get(0).getPriorityScore());
        assertTrue(sorted.get(0).getPriorityScore()
                >= sorted.get(1).getPriorityScore());
    }
}
