package drs.shared.strategy;

import drs.shared.enums.CapSeverity;
import drs.shared.model.Incident;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LifeRiskFirstStrategy}.
  
 */
class LifeRiskFirstStrategyTest {

    @Test
    void testManyPeopleAffectedDominates() {
        Incident i = new Incident();
        i.setCapSeverity(CapSeverity.MINOR);
        i.setPeopleAffected(50);
        // 50*10 + 2*5 = 510
        assertEquals(510, new LifeRiskFirstStrategy().calculateScore(i));
    }
}
