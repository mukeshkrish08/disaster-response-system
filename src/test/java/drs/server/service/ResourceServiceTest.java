package drs.server.service;

import drs.shared.enums.ResourceStatus;
import drs.shared.enums.ResourceType;
import drs.shared.model.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Resource model constructor logic used by
 * {@link ResourceService}. Full service-level tests require a running
 * database and are covered by the integration test suite.
  
 */
class ResourceServiceTest {

    @Test
    void testNewResourceStartsFullyAvailable() {
        Resource r = new Resource("RES-2026-0001", "Fire Truck",
                ResourceType.VEHICLE, 5, null);
        assertEquals(5, r.getQuantityTotal());
        assertEquals(5, r.getQuantityAvailable());
        assertEquals(ResourceStatus.AVAILABLE, r.getStatus());
    }

    @Test
    void testResourceCodeAndNameSet() {
        Resource r = new Resource("RES-2026-0002", "Medical Kit",
                ResourceType.MEDICAL_SUPPLY, 50, 1);
        assertEquals("RES-2026-0002", r.getResourceCode());
        assertEquals("Medical Kit", r.getResourceName());
        assertEquals(ResourceType.MEDICAL_SUPPLY, r.getResourceType());
        assertEquals(Integer.valueOf(1), r.getHomeLocationPk());
    }

    @Test
    void testTimestampsAreSet() {
        Resource r = new Resource("RES-2026-0003", "Generator",
                ResourceType.EQUIPMENT, 3, null);
        assertNotNull(r.getCreatedAt());
        assertNotNull(r.getUpdatedAt());
    }

    @Test
    void testToStringIncludesQuantities() {
        Resource r = new Resource("RES-2026-0004", "Sandbag Pallet",
                ResourceType.EQUIPMENT, 100, null);
        String s = r.toString();
        assertTrue(s.contains("100/100"),
                "Expected quantities in toString, got: " + s);
    }
}
