package drs.server;

import drs.server.router.AdminRequestHandler;
import drs.server.router.AssignmentRequestHandler;
import drs.server.router.AuthRequestHandler;
import drs.server.router.DamageRecoveryRequestHandler;
import drs.server.router.IncidentRequestHandler;
import drs.server.router.NotificationRequestHandler;
import drs.server.router.RequestHandler;
import drs.server.router.ResourceRequestHandler;
import drs.shared.exception.AuthorizationException;
import drs.shared.exception.AuthenticationException;
import drs.shared.exception.DataAccessException;
import drs.shared.exception.DrsException;
import drs.shared.exception.InvalidStateTransitionException;
import drs.shared.exception.SessionExpiredException;
import drs.shared.exception.ValidationException;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;

/**
 * Dispatches each Request to the correct per-feature handler using an
 * EnumMap of {@link OperationType} -> {@link RequestHandler}, giving
 * O(1) routing.
 *
 * Implements the Factory + Command pattern combo: handlers are created
 * once at startup; each Request is treated as a Command and routed.
  
 */
public class RequestRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestRouter.class);

    private final EnumMap<OperationType, RequestHandler> handlers;

    public RequestRouter(ServerApplicationContext context) {
        this.handlers = new EnumMap<>(OperationType.class);
        registerHandlers(context);
    }

    /*   * Route a request to the matching handler.
         * @param request inbound request (non-null)
     * @param session active session (null for LOGIN)
     * @return Response built by the handler, or an error Response if
     *        the operation type is unknown or the handler threw
     */
    public Response route(Request request, Session session) {
        if (request == null || request.getOperation() == null) {
            return Response.error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                    "Missing operation");
        }
        RequestHandler handler = handlers.get(request.getOperation());
        if (handler == null) {
            return Response.error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                    "Unknown operation: " + request.getOperation());
        }
        try {
            return handler.handle(request, session);
        } catch (SessionExpiredException e) {
            return Response.error(ProtocolConstants.ERR_SESSION_EXPIRED,
                    e.getMessage());
        } catch (AuthorizationException e) {
            return Response.error(ProtocolConstants.ERR_AUTHZ_DENIED,
                    e.getMessage());
        } catch (AuthenticationException e) {
            return Response.error(ProtocolConstants.ERR_AUTH_FAILED,
                    e.getMessage());
        } catch (ValidationException e) {
            return Response.error(ProtocolConstants.ERR_VALIDATION,
                    e.getMessage());
        } catch (InvalidStateTransitionException e) {
            return Response.error(ProtocolConstants.ERR_INVALID_TRANSITION,
                    e.getMessage());
        } catch (DataAccessException e) {
            LOG.error("Data access error processing {}", request.getOperation(), e);
            return Response.error(ProtocolConstants.ERR_DATA_ACCESS,
                    "A database error occurred.");
        } catch (DrsException e) {
            return Response.error(ProtocolConstants.ERR_INTERNAL, e.getMessage());
        } catch (Exception e) {
            LOG.error("Unhandled error processing {}", request.getOperation(), e);
            return Response.error(ProtocolConstants.ERR_INTERNAL,
                    "An unexpected error occurred.");
        }
    }

    private void registerHandlers(ServerApplicationContext context) {
        AuthRequestHandler auth = new AuthRequestHandler(context);
        IncidentRequestHandler incident = new IncidentRequestHandler(context);
        AssignmentRequestHandler assignment = new AssignmentRequestHandler(context);
        NotificationRequestHandler notification = new NotificationRequestHandler(context);
        ResourceRequestHandler resource = new ResourceRequestHandler(context);
        DamageRecoveryRequestHandler damage = new DamageRecoveryRequestHandler(context);
        AdminRequestHandler admin = new AdminRequestHandler(context);

        // Auth
        handlers.put(OperationType.LOGIN, auth);
        handlers.put(OperationType.LOGOUT, auth);
        handlers.put(OperationType.REGISTER_CITIZEN, auth);

        // Incidents
        handlers.put(OperationType.REPORT_INCIDENT, incident);
        handlers.put(OperationType.ASSESS_INCIDENT, incident);
        handlers.put(OperationType.REJECT_INCIDENT, incident);
        handlers.put(OperationType.WITHDRAW_INCIDENT, incident);
        handlers.put(OperationType.CLOSE_INCIDENT, incident);
        handlers.put(OperationType.LIST_INCIDENTS, incident);
        handlers.put(OperationType.GET_INCIDENT_DETAILS, incident);
        handlers.put(OperationType.LIST_MY_REPORTS, incident);
        handlers.put(OperationType.SWITCH_PRIORITY_STRATEGY, incident);
        handlers.put(OperationType.GET_CURRENT_STRATEGY, incident);
        handlers.put(OperationType.GET_INCIDENT_HISTORY, incident);

        // Assignments
        handlers.put(OperationType.SUGGEST_TEAMS, assignment);
        handlers.put(OperationType.ASSIGN_TEAMS, assignment);
        handlers.put(OperationType.START_RESPONSE, assignment);
        handlers.put(OperationType.COMPLETE_RESPONSE, assignment);
        handlers.put(OperationType.LIST_INCIDENTS_BY_TEAM_LEADER, assignment);
        handlers.put(OperationType.LIST_AVAILABLE_TEAMS, assignment);

        // Notifications
        handlers.put(OperationType.LIST_NOTIFICATIONS, notification);
        handlers.put(OperationType.ACKNOWLEDGE_NOTIFICATION, notification);

        // Resources (Feature 1)
        handlers.put(OperationType.LIST_RESOURCES, resource);
        handlers.put(OperationType.ADD_RESOURCE, resource);
        handlers.put(OperationType.ALLOCATE_RESOURCE, resource);
        handlers.put(OperationType.RETURN_ALLOCATION, resource);
        handlers.put(OperationType.LIST_ALLOCATIONS_FOR_INCIDENT, resource);
        // Resource lifecycle: maintenance + retirement
        handlers.put(OperationType.SEND_RESOURCE_TO_MAINTENANCE, resource);
        handlers.put(OperationType.RETURN_RESOURCE_FROM_MAINTENANCE, resource);
        handlers.put(OperationType.RETIRE_RESOURCE, resource);

        // Damage / Recovery (Feature 2)
        handlers.put(OperationType.RECORD_DAMAGE_ASSESSMENT, damage);
        handlers.put(OperationType.LIST_DAMAGE_ASSESSMENTS, damage);
        handlers.put(OperationType.CREATE_RECOVERY_TASK, damage);
        handlers.put(OperationType.UPDATE_RECOVERY_TASK_STATUS, damage);
        handlers.put(OperationType.LIST_RECOVERY_TASKS, damage);
        handlers.put(OperationType.LIST_MY_RECOVERY_TASKS, damage);

        // Admin reads
        handlers.put(OperationType.LIST_USERS, admin);
        handlers.put(OperationType.LIST_DEPARTMENTS, admin);
        handlers.put(OperationType.LIST_TEAMS, admin);
        handlers.put(OperationType.LIST_AUDIT_LOG, admin);
        handlers.put(OperationType.VERIFY_AUDIT_CHAIN, admin);
        handlers.put(OperationType.LIST_LOCATIONS, admin);
        // Admin writes: create + deactivate for users/teams/depts/locations
        handlers.put(OperationType.CREATE_STAFF_USER, admin);
        handlers.put(OperationType.DEACTIVATE_USER, admin);
        handlers.put(OperationType.ADD_TEAM, admin);
        handlers.put(OperationType.DEACTIVATE_TEAM, admin);
        handlers.put(OperationType.ADD_DEPARTMENT, admin);
        handlers.put(OperationType.DEACTIVATE_DEPARTMENT, admin);
        handlers.put(OperationType.ADD_LOCATION, admin);
        handlers.put(OperationType.DEACTIVATE_LOCATION, admin);

        LOG.info("Registered {} operation handlers", handlers.size());
    }
}
