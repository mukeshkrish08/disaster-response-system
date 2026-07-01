package drs.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InputValidator}.
 *
 * Includes parameterized boundary tests for email, quantity, and geographic
 * coordinate validation. Parameterized tests follow the JUnit Jupiter style
 * recommended by Vogel (2026) and documented in the JUnit User Guide
 * (JUnit Team, 2025).
  
 */
class InputValidatorTest {

    @Test
    void testValidEmailAccepted() {
        assertTrue(InputValidator.validateEmail("user@example.com"));
        assertTrue(InputValidator.validateEmail("first.last+tag@uni.edu.au"));
    }

    @Test
    void testInvalidEmailRejected() {
        assertFalse(InputValidator.validateEmail("not-an-email"));
        assertFalse(InputValidator.validateEmail(""));
        assertFalse(InputValidator.validateEmail(null));
    }

    @Test
    void testStrongPasswordAccepted() {
        assertTrue(InputValidator.validatePassword("Demo@123"));
        assertTrue(InputValidator.validatePassword("Str0ng!Pass"));
    }

    @Test
    void testWeakPasswordRejected() {
        assertFalse(InputValidator.validatePassword("short"));
        assertFalse(InputValidator.validatePassword("alllowercase1!"));  // no uppercase
        assertFalse(InputValidator.validatePassword("NoDigits!"));
        assertFalse(InputValidator.validatePassword("NoSpecial1"));
    }

    @Test
    void testDescriptionLength() {
        assertTrue(InputValidator.validateDescription("This is a fire"));
        assertFalse(InputValidator.validateDescription("bad"));        // < 5
        assertFalse(InputValidator.validateDescription(null));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1100; i++) sb.append("x");
        assertFalse(InputValidator.validateDescription(sb.toString()));
    }

    /*   * Parameterized boundary check: every value here should be rejected
     * by validateEmail. Covers the equivalence partitions: empty, no @,
     * leading @, trailing @, no domain dot, whitespace, single chars.
     */
    @ParameterizedTest(name = "rejects email: \"{0}\"")
    @ValueSource(strings = {
            "",
            "plain",
            "@nodomain.com",
            "noatsign.com",
            "user@",
            "user@@double.com",
            "spaces in@email.com",
            "a",
            "user@domain"
    })
    void testParameterizedInvalidEmailsRejected(String email) {
        assertFalse(InputValidator.validateEmail(email));
    }

    /*   * Parameterized boundary check for quantity validation.
     * Quantities must be positive integers. Zero and negative are invalid.
     */
    @ParameterizedTest(name = "rejects quantity: {0}")
    @ValueSource(ints = { 0, -1, -100, Integer.MIN_VALUE })
    void testParameterizedInvalidQuantitiesRejected(int quantity) {
        assertFalse(InputValidator.validateQuantity(quantity));
    }

    @ParameterizedTest(name = "accepts quantity: {0}")
    @ValueSource(ints = { 1, 5, 100, 1000, 10000 })
    void testParameterizedValidQuantitiesAccepted(int quantity) {
        assertTrue(InputValidator.validateQuantity(quantity));
    }

    /*   * Latitude must be in the range [-90, 90]. Longitude must be in
     * [-180, 180]. Boundary cases are explicitly listed below.
     */
    @ParameterizedTest(name = "valid lat={0}, lon={1}")
    @CsvSource({
            // Sydney CBD
            "-33.8688, 151.2093",
            // North Pole boundary
            " 90.0,      0.0",
            // South Pole boundary
            "-90.0,      0.0",
            // International date line boundary
            "  0.0,    180.0",
            "  0.0,   -180.0",
            // Equator / prime meridian
            "  0.0,      0.0"
    })
    void testParameterizedValidCoordinates(double lat, double lon) {
        assertTrue(InputValidator.validateLatitude(lat));
        assertTrue(InputValidator.validateLongitude(lon));
    }

    @ParameterizedTest(name = "invalid lat={0}, lon={1}")
    @CsvSource({
            // Out-of-range latitudes
            " 90.1,  0.0",
            "-90.1,  0.0",
            "180.0,  0.0",
            // Out-of-range longitudes
            "  0.0, 180.1",
            "  0.0,-180.1",
            "  0.0, 360.0"
    })
    void testParameterizedInvalidCoordinatesRejected(double lat, double lon) {
        // At least one of the two should be rejected.
        assertFalse(InputValidator.validateLatitude(lat)
                && InputValidator.validateLongitude(lon));
    }
}
