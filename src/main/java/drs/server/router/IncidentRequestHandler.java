package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.enums.CapCertainty;
import drs.shared.enums.CapSeverity;
import drs.shared.enums.CapUrgency;
import drs.shared.enums.DisasterType;
import drs.shared.enums.UserRole;
import drs.shared.model.Incident;
import drs.shared.model.IncidentUpdate;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all incident-related operations: report, assess, reject,
 * close, list, get details, switch priority strategy.
  
 */
public class IncidentRequestHandler extends AbstractRequestHandler {

    public IncidentRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        switch (op) {
            case REPORT_INCIDENT:           return report(request, session);
            case ASSESS_INCIDENT:           return assess(request, session);
            case REJECT_INCIDENT:           return reject(request, session);
            case WITHDRAW_INCIDENT:         return withdraw(request, session);
            case CLOSE_INCIDENT:            return close(request, session);
            case LIST_INCIDENTS:            return listAll(request, session);
            case GET_INCIDENT_DETAILS:      return details(request, session);
            case LIST_MY_REPORTS:           return listMyReports(request, session);
            case SWITCH_PRIORITY_STRATEGY:  return switchStrategy(request, session);
            case GET_CURRENT_STRATEGY:      return getCurrentStrategy(session);
            case GET_INCIDENT_HISTORY:      return history(request, session);
            default:
                return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                        "Unsupported operation: " + op);
        }
    }

    private Response report(Request request, Session session) {
        authz.requireAuthenticated(session);
        DisasterType disasterType = DisasterType.valueOf(
                (String) request.get("disasterType"));
        String locationCode = request.get("locationCode");
        Number lat = request.get("latitude");
        Number lon = request.get("longitude");
        String description = request.get("description");
        String contactPhone = request.get("contactPhone");
        Number people = request.get("peopleAffected");
        String propertyRisk = request.get("propertyRiskLevel");

        Incident incident = context.getIncidentService().reportIncident(
                session.getUserCode(), disasterType, locationCode,
                lat.doubleValue(), lon.doubleValue(), description,
                contactPhone,
                people == null ? 0 : people.intValue(),
                propertyRisk, session.getClientIp());
        return ok(incident);
    }

    private Response assess(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String code = request.get("incidentCode");
        CapSeverity sev = CapSeverity.valueOf((String) request.get("severity"));
        CapUrgency urg = CapUrgency.valueOf((String) request.get("urgency"));
        CapCertainty cer = CapCertainty.valueOf((String) request.get("certainty"));

        Incident incident = context.getIncidentService().assessIncident(
                code, sev, urg, cer, session.getUserCode(),
                session.getClientIp());
        return ok(incident);
    }

    private Response reject(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String code = request.get("incidentCode");
        String reason = request.get("reason");
        Incident incident = context.getIncidentService().rejectIncident(
                code, session.getUserCode(), reason, session.getClientIp());
        return ok(incident);
    }

    /*   * Citizen self-service withdrawal. Authentication required -
     * ownership check happens inside IncidentService.
     */
    private Response withdraw(Request request, Session session) {
        authz.requireAuthenticated(session);
        String code = request.get("incidentCode");
        String reason = request.get("reason");
        Incident incident = context.getIncidentService().withdrawIncident(
                code, session.getUserCode(), reason, session.getClientIp());
        return ok(incident);
    }

    private Response close(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String code = request.get("incidentCode");
        Incident incident = context.getIncidentService().closeIncident(
                code, session.getUserCode(), session.getClientIp());
        return ok(incident);
    }

    private Response listAll(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR,
                UserRole.TEAM_LEADER, UserRole.ADMIN);
        List<Incident> incidents = context.getIncidentService().getAllIncidents();
        return ok((java.io.Serializable) (java.util.ArrayList<Incident>)
                new java.util.ArrayList<>(incidents));
    }

    private Response details(Request request, Session session) {
        authz.requireAuthenticated(session);
        String code = request.get("incidentCode");
        Incident incident = context.getIncidentService().getIncident(code)
                .orElse(null);
        if (incident == null) {
            return error(ProtocolConstants.ERR_NOT_FOUND,
                    "Incident not found: " + code);
        }
        return ok(incident);
    }

    private Response listMyReports(Request request, Session session) {
        authz.requireAuthenticated(session);
        List<Incident> mine = context.getIncidentService()
                .getIncidentsByReporter(session.getUserCode());
        return ok(new java.util.ArrayList<>(mine));
    }

    private Response switchStrategy(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String strategyName = request.get("strategyName");
        context.getPriorityService().switchStrategy(strategyName);
        context.getIncidentService().recalculateAllPriorities();
        context.getAuditService().logAction(session.getUserPk(),
                "SWITCH_PRIORITY_STRATEGY", null, null,
                "new=" + context.getPriorityService().getCurrentStrategyName(),
                session.getClientIp(), true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("strategyName",
                context.getPriorityService().getCurrentStrategyName());
        return ok(payload);
    }

    private Response getCurrentStrategy(Session session) {
        authz.requireAuthenticated(session);
        Map<String, Object> payload = new HashMap<>();
        payload.put("strategyName",
                context.getPriorityService().getCurrentStrategyName());
        return ok(payload);
    }

    private Response history(Request request, Session session) {
        authz.requireAuthenticated(session);
        String code = request.get("incidentCode");
        Incident incident = context.getIncidentService().getIncident(code)
                .orElse(null);
        if (incident == null) {
            return error(ProtocolConstants.ERR_NOT_FOUND,
                    "Incident not found: " + code);
        }
        List<IncidentUpdate> updates = context.getUpdateRepository()
                .findByIncidentPk(incident.getIncidentPk());
        return ok(new java.util.ArrayList<>(updates));
    }
}
