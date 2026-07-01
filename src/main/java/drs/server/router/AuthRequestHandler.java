package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles LOGIN and LOGOUT.
  
 */
public class AuthRequestHandler extends AbstractRequestHandler {

    public AuthRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        if (op == OperationType.LOGIN) {
            return handleLogin(request);
        }
        if (op == OperationType.LOGOUT) {
            return handleLogout(request, session);
        }
        if (op == OperationType.REGISTER_CITIZEN) {
            return handleRegisterCitizen(request);
        }
        return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                "Unsupported operation: " + op);
    }

    private Response handleLogin(Request request) {
        String email = request.get("email");
        String password = request.get("password");
        String clientIp = request.get("clientIp");

        User user = context.getAuthenticationService()
                .authenticate(email, password, clientIp);
        Session newSession = context.getSessionManager()
                .createSession(user, clientIp);

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", newSession.getToken());
        payload.put("user", user);
        return ok(payload);
    }

    private Response handleLogout(Request request, Session session) {
        if (session == null) {
            return ok();
        }
        context.getSessionManager().invalidate(session.getToken());
        context.getAuditService().logAction(session.getUserPk(),
                "LOGOUT", "User", session.getUserCode(), null,
                session.getClientIp(), true);
        return ok();
    }

    /*   * Public, anonymous endpoint for citizen self-registration. The
     * {@link drs.server.service.AuthenticationService#registerCitizen}
     * call hardcodes the role to CITIZEN so a client cannot escalate.
     */
    private Response handleRegisterCitizen(Request request) {
        String fullName = request.get("fullName");
        String email = request.get("email");
        String password = request.get("password");
        String clientIp = request.get("clientIp");

        User user = context.getAuthenticationService()
                .registerCitizen(fullName, email, password, clientIp);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user", user);
        payload.put("message",
                "Account created. You can now sign in with your email.");
        return ok(payload);
    }
}
