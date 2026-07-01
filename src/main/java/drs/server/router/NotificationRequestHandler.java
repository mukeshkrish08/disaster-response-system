package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.enums.UserRole;
import drs.shared.model.Notification;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles notification operations.
  
 */
public class NotificationRequestHandler extends AbstractRequestHandler {

    public NotificationRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        if (op == OperationType.LIST_NOTIFICATIONS) {
            authz.requireAnyRole(session, UserRole.AGENCY_REP, UserRole.ADMIN);
            List<Notification> list = context.getNotificationService()
                    .getNotificationsForUser(session.getUserCode());
            return ok(new ArrayList<>(list));
        }
        if (op == OperationType.ACKNOWLEDGE_NOTIFICATION) {
            authz.requireAnyRole(session, UserRole.AGENCY_REP, UserRole.ADMIN);
            String code = request.get("notificationCode");
            context.getNotificationService().acknowledgeNotification(
                    code, session.getUserCode(), session.getClientIp());
            return ok();
        }
        return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                "Unsupported operation: " + op);
    }
}
