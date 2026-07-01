package drs.server;

import drs.shared.enums.UserRole;
import drs.shared.exception.AuthorizationException;
import drs.shared.exception.SessionExpiredException;
import drs.shared.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AuthorizationService}.
  
 */
class AuthorizationServiceTest {

    private Session sessionWithRole(UserRole role) {
        User u = new User();
        u.setUserPk(1);
        u.setUserCode("USR-2026-0001");
        u.setFullName("Test");
        u.setRole(role);
        return new Session("token-abc", u, "127.0.0.1");
    }

    @Test
    void testRequireAuthenticatedThrowsOnNull() {
        assertThrows(SessionExpiredException.class, () ->
                new AuthorizationService().requireAuthenticated(null));
    }

    @Test
    void testRequireRoleAllowsMatch() {
        AuthorizationService a = new AuthorizationService();
        assertDoesNotThrow(() -> a.requireRole(
                sessionWithRole(UserRole.ADMIN), UserRole.ADMIN));
    }

    @Test
    void testRequireRoleDeniesMismatch() {
        AuthorizationService a = new AuthorizationService();
        assertThrows(AuthorizationException.class, () -> a.requireRole(
                sessionWithRole(UserRole.CITIZEN), UserRole.ADMIN));
    }

    @Test
    void testRequireAnyRolePassesWithMatch() {
        AuthorizationService a = new AuthorizationService();
        assertDoesNotThrow(() -> a.requireAnyRole(
                sessionWithRole(UserRole.COORDINATOR),
                UserRole.COORDINATOR, UserRole.ADMIN));
    }

    @Test
    void testRequireAnyRoleDeniesIfNoneMatch() {
        AuthorizationService a = new AuthorizationService();
        assertThrows(AuthorizationException.class, () -> a.requireAnyRole(
                sessionWithRole(UserRole.CITIZEN),
                UserRole.COORDINATOR, UserRole.ADMIN));
    }
}
