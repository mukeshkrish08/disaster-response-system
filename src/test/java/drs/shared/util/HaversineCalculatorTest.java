package drs.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HaversineCalculator}.
 *
 * Includes parameterized geographic distance checks across known NSW
 * city pairs. Parameterized tests in JUnit Jupiter are documented by
 * the JUnit User Guide (JUnit Team, 2025).
  
 */
class HaversineCalculatorTest {

    @Test
    void testZeroDistanceForSamePoint() {
        double d = HaversineCalculator.calculateDistance(
                -33.8688, 151.2093, -33.8688, 151.2093);
        assertEquals(0.0, d, 0.001);
    }

    @Test
    void testSydneyToParramattaApprox23km() {
        // Sydney CBD to Parramatta is ~23 km
        double d = HaversineCalculator.calculateDistance(
                -33.8688, 151.2093, -33.8150, 151.0000);
        assertTrue(d > 18 && d < 28,
                "Expected ~23km Sydney-Parramatta, got " + d);
    }

    /*   * Distance check for known NSW location pairs. Each row gives the
     * coordinates of two NSW cities and the expected range of the
     * great-circle distance in kilometres. Ranges are deliberately
     * loose to allow for Earth-radius rounding and the great-circle
     * approximation, while still being tight enough to detect bugs.
     */
    @ParameterizedTest(name = "{0} -> {1} expected {2}..{3} km")
    @CsvSource({
            // Sydney CBD          Penrith (~50km west)
            "'Sydney',    'Penrith',     -33.8688, 151.2093, -33.7510, 150.6940,  45,  60",
            // Sydney CBD          Wollongong (~70km south)
            "'Sydney',    'Wollongong',  -33.8688, 151.2093, -34.4278, 150.8931,  65,  85",
            // Sydney CBD          Newcastle (~120km north)
            "'Sydney',    'Newcastle',   -33.8688, 151.2093, -32.9283, 151.7817, 110, 135",
            // Hornsby             Cronulla (~40km diagonal)
            "'Hornsby',   'Cronulla',    -33.7036, 151.0987, -34.0581, 151.1521,  35,  50",
            // Westmead            Liverpool (~13km southwest)
            "'Westmead',  'Liverpool',   -33.8067, 150.9876, -33.9200, 150.9230,  10,  18",
            // Bondi               Sydney CBD (~6km)
            "'Bondi',     'Sydney',      -33.8908, 151.2743, -33.8688, 151.2093,   4,   9"
    })
    void testParameterizedNswCityDistances(String from, String to,
                                           double lat1, double lon1,
                                           double lat2, double lon2,
                                           double minKm, double maxKm) {
        double d = HaversineCalculator.calculateDistance(lat1, lon1, lat2, lon2);
        assertTrue(d >= minKm && d <= maxKm,
                String.format("Expected %s -> %s in [%.0f, %.0f] km, got %.2f",
                        from, to, minKm, maxKm, d));
    }

    /*   * Distance is symmetric: A to B equals B to A. Tested across a
     * range of NSW coordinate pairs to confirm the implementation has
     * no directional bug.
     */
    @ParameterizedTest(name = "symmetry: ({0},{1}) <-> ({2},{3})")
    @CsvSource({
            "-33.8688, 151.2093, -33.7510, 150.6940",
            "-33.8688, 151.2093, -34.4278, 150.8931",
            "-33.7036, 151.0987, -34.0581, 151.1521",
            "-33.8067, 150.9876, -33.9200, 150.9230"
    })
    void testParameterizedDistanceIsSymmetric(double lat1, double lon1,
                                              double lat2, double lon2) {
        double forward = HaversineCalculator.calculateDistance(
                lat1, lon1, lat2, lon2);
        double reverse = HaversineCalculator.calculateDistance(
                lat2, lon2, lat1, lon1);
        assertEquals(forward, reverse, 0.001,
                "Haversine distance should be symmetric");
    }
}
