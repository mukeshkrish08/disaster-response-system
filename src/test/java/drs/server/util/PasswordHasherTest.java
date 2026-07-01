package drs.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PasswordHasher} (BCrypt).
  
 */
class PasswordHasherTest {

    @Test
    void testHashAndVerifyRoundTrip() {
        String hash = PasswordHasher.hash("Demo@123");
        assertTrue(PasswordHasher.verify("Demo@123", hash));
    }

    @Test
    void testVerifyWrongPasswordFails() {
        String hash = PasswordHasher.hash("Demo@123");
        assertFalse(PasswordHasher.verify("Wrong!1", hash));
    }

    @Test
    void testHashIsSaltedAndDifferentEachCall() {
        String h1 = PasswordHasher.hash("Demo@123");
        String h2 = PasswordHasher.hash("Demo@123");
        assertNotEquals(h1, h2);
    }
}
