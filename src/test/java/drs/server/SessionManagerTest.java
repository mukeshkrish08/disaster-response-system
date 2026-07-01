package drs.server;

import drs.shared.enums.UserRole;
import drs.shared.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SessionManager}.
  
 */
class SessionManagerTest {

    private User buildUser(String code, UserRole role) {
        User u = new User();
        u.setUserPk(1);
        u.setUserCode(code);
        u.setFullName("Test User");
        u.setRole(role);
        return u;
    }

    @Test
    void testCreateSessionReturnsValidToken() {
        SessionManager sm = new SessionManager(30);
        Session s = sm.createSession(buildUser("USR-2026-0001",
                UserRole.COORDINATOR), "127.0.0.1");
        assertNotNull(s.getToken());
        assertEquals(36, s.getToken().length()); // UUID
        sm.shutdown();
    }

    @Test
    void testValidateTokenReturnsSession() {
        SessionManager sm = new SessionManager(30);
        Session s = sm.createSession(buildUser("USR-2026-0001",
                UserRole.COORDINATOR), "127.0.0.1");
        Optional<Session> found = sm.validateToken(s.getToken());
        assertTrue(found.isPresent());
        assertEquals(s.getToken(), found.get().getToken());
        sm.shutdown();
    }

    @Test
    void testInvalidateRemovesSession() {
        SessionManager sm = new SessionManager(30);
        Session s = sm.createSession(buildUser("USR-2026-0001",
                UserRole.CITIZEN), "127.0.0.1");
        sm.invalidate(s.getToken());
        assertFalse(sm.validateToken(s.getToken()).isPresent());
        sm.shutdown();
    }

    @Test
    void testActiveCountReflectsState() {
        SessionManager sm = new SessionManager(30);
        sm.createSession(buildUser("USR-2026-0001",
                UserRole.CITIZEN), "127.0.0.1");
        sm.createSession(buildUser("USR-2026-0002",
                UserRole.CITIZEN), "127.0.0.1");
        assertEquals(2, sm.activeSessionCount());
        sm.shutdown();
    }
}
