package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.enums.UserRole;
import drs.shared.model.Incident;
import drs.shared.model.IncidentAssignment;
import drs.shared.model.ResponseTeam;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.exception.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles team-assignment operations: suggest, assign, start, complete.
  
 */
public class AssignmentRequestHandler extends AbstractRequestHandler {

    public AssignmentRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        switch (op) {
            case SUGGEST_TEAMS:                  return suggestTeams(request, session);
            case ASSIGN_TEAMS:                   return assignTeams(request, session);
            case START_RESPONSE:                 return startResponse(request, session);
            case COMPLETE_RESPONSE:              return completeResponse(request, session);
            case LIST_INCIDENTS_BY_TEAM_LEADER:  return listByLeader(session);
            case LIST_AVAILABLE_TEAMS:           return listAvailable(session);
            default:
                return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                        "Unsupported operation: " + op);
        }
    }

    private Response suggestTeams(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String code = request.get("incidentCode");
        Incident incident = context.getIncidentService().getIncident(code)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + code));

        ResponseTeam primary = context.getGeoDispatchService()
                .suggestPrimaryTeam(incident);
        Optional<ResponseTeam> secondary = context.getGeoDispatchService()
                .suggestSecondaryTeam(incident, primary.getDepartmentType());

        double primaryDistance = context.getGeoDispatchService()
                .calculateDistance(incident.getIncidentLat(),
                        incident.getIncidentLon(),
                        primary.getLatitude(), primary.getLongitude());

        Map<String, Object> payload = new HashMap<>();
        payload.put("primaryTeam", primary);
        payload.put("primaryDistanceKm", primaryDistance);
        if (secondary.isPresent()) {
            ResponseTeam s = secondary.get();
            double secondaryDistance = context.getGeoDispatchService()
                    .calculateDistance(incident.getIncidentLat(),
                            incident.getIncidentLon(),
                            s.getLatitude(), s.getLongitude());
            payload.put("secondaryTeam", s);
            payload.put("secondaryDistanceKm", secondaryDistance);
        }
        return ok(payload);
    }

    private Response assignTeams(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String incidentCode = request.get("incidentCode");
        String primaryCode = request.get("primaryTeamCode");
        String secondaryCode = request.get("secondaryTeamCode");
        Number primaryDistance = request.get("primaryDistanceKm");
        Number secondaryDistance = request.get("secondaryDistanceKm");

        context.getAssignmentService().assignTeams(
                incidentCode, primaryCode, secondaryCode,
                primaryDistance == null ? 0.0 : primaryDistance.doubleValue(),
                secondaryDistance == null ? 0.0 : secondaryDistance.doubleValue(),
                session.getUserCode(), session.getClientIp());
        return ok();
    }

    private Response startResponse(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.TEAM_LEADER, UserRole.ADMIN);
        String code = request.get("incidentCode");
        context.getAssignmentService().startResponse(code,
                session.getUserCode(), session.getClientIp());
        return ok();
    }

    private Response completeResponse(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.TEAM_LEADER, UserRole.ADMIN);
        String code = request.get("incidentCode");
        String notes = request.get("notes");
        context.getAssignmentService().completeResponse(code,
                session.getUserCode(), notes, session.getClientIp());
        return ok();
    }

    private Response listByLeader(Session session) {
        authz.requireAnyRole(session, UserRole.TEAM_LEADER, UserRole.ADMIN);
        List<IncidentAssignment> list = context.getAssignmentRepository()
                .findByLeaderUserPk(session.getUserPk());
        return ok(new ArrayList<>(list));
    }

    private Response listAvailable(Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        List<ResponseTeam> teams = context.getResponseTeamRepository()
                .findAvailable();
        return ok(new ArrayList<>(teams));
    }
}
