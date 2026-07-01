package drs.server.service;

import drs.shared.enums.DamageLevel;
import drs.shared.enums.InfrastructureStatus;
import drs.shared.model.DamageAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link DamageAssessment} defaults used by
 * {@link DamageAssessmentService}.
  
 */
class DamageAssessmentServiceTest {

    @Test
    void testDefaultsAreOperational() {
        DamageAssessment a = new DamageAssessment();
        assertEquals(DamageLevel.NONE, a.getBuildingDamageLevel());
        assertEquals(InfrastructureStatus.OPERATIONAL, a.getRoadStatus());
        assertEquals(InfrastructureStatus.OPERATIONAL, a.getPowerStatus());
        assertEquals(InfrastructureStatus.OPERATIONAL, a.getWaterStatus());
    }

    @Test
    void testAllDamageLevelsHaveDisplayNames() {
        for (DamageLevel l : DamageLevel.values()) {
            assertNotNull(l.displayName());
        }
    }
}
