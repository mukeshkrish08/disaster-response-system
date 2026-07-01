package drs.server;

import drs.shared.enums.UserRole;
import drs.shared.exception.AuthorizationException;
import drs.shared.exception.SessionExpiredException;

/**
 * Server-side role-based access control. Every privileged request
 * handler should call one of the {@code require*} methods at entry.
  
 */
public class AuthorizationService {

    /*   * Require a non-null session (authenticated user).
         * @param session candidate session (nullable)
     * @throws SessionExpiredException if session is null
     */
    public void requireAuthenticated(Session session) {
        if (session == null) {
            throw new SessionExpiredException(
                "Your session has expired. Please sign in again.");
        }
    }

    /*   * Require the session belongs to a user with the given role.
         * @param session active session
     * @param role    required role
     * @throws AuthorizationException if role does not match
     */
    public void requireRole(Session session, UserRole role) {
        requireAuthenticated(session);
        if (session.getRole() != role) {
            throw new AuthorizationException(
                "This action requires role " + role.displayName()
                + " (you are " + session.getRole().displayName() + ").");
        }
    }

    /*   * Require the session has one of the given roles.
         * @param session active session
     * @param roles   any one of these is acceptable
     * @throws AuthorizationException if none match
     */
    public void requireAnyRole(Session session, UserRole... roles) {
        requireAuthenticated(session);
        for (UserRole r : roles) {
            if (session.getRole() == r) {
                return;
            }
        }
        StringBuilder sb = new StringBuilder("This action requires one of: ");
        for (int i = 0; i < roles.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(roles[i].displayName());
        }
        sb.append(" (you are ").append(session.getRole().displayName()).append(").");
        throw new AuthorizationException(sb.toString());
    }

    /*   * Allow the user to act on their own user code OR if they have one
     * of the given fallback roles.
         * @param session     active session
     * @param userCode    target user code
     * @param fallbackRoles roles that bypass the self-only check
     */
    public void requireSelfOrAnyRole(Session session, String userCode,
                                     UserRole... fallbackRoles) {
        requireAuthenticated(session);
        if (userCode != null && userCode.equals(session.getUserCode())) {
            return;
        }
        requireAnyRole(session, fallbackRoles);
    }
}
