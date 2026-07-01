package drs.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link IdGenerator}.
 *
 * Note: when run without a database, the generator starts each prefix
 * counter at 0; that's fine for unit testing the format and uniqueness.
  
 */
class IdGeneratorTest {

    @Test
    void testIncidentCodeFormat() {
        String code = IdGenerator.generateIncidentCode();
        assertTrue(code.startsWith("INC-"),
                "Expected INC- prefix, got " + code);
        // Format: PREFIX-YYYY-####
        assertTrue(code.matches("INC-\\d{4}-\\d{4,}"),
                "Format mismatch: " + code);
    }

    @Test
    void testUniqueAcrossCalls() {
        String a = IdGenerator.generateIncidentCode();
        String b = IdGenerator.generateIncidentCode();
        assertNotEquals(a, b);
    }

    @Test
    void testDifferentPrefixesDontCollide() {
        String userCode = IdGenerator.generateUserCode();
        String taskCode = IdGenerator.generateTaskCode();
        assertTrue(userCode.startsWith("USR-"));
        assertTrue(taskCode.startsWith("RTK-"));
    }
}
