package drs.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link GeoLocation}.
  
 */
class GeoLocationTest {

    @Test
    void testEqualityByCoordinates() {
        GeoLocation a = new GeoLocation(-33.8688, 151.2093);
        GeoLocation b = new GeoLocation(-33.8688, 151.2093);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testInequality() {
        GeoLocation a = new GeoLocation(-33.8688, 151.2093);
        GeoLocation b = new GeoLocation(-33.8688, 151.0000);
        assertNotEquals(a, b);
    }
}
