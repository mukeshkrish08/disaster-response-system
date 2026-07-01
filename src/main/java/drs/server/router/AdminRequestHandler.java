package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.enums.UserRole;
import drs.shared.model.AuditLog;
import drs.shared.model.Department;
import drs.shared.model.Location;
import drs.shared.model.ResponseTeam;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrative read-only operations. Used by the admin panel.
  
 */
public class AdminRequestHandler extends AbstractRequestHandler {

    public AdminRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        switch (op) {
            case LIST_USERS:                return listUsers(session);
            case LIST_DEPARTMENTS:          return listDepartments(session);
            case LIST_TEAMS:                return listTeams(session);
            case LIST_LOCATIONS:            return listLocations(session);
            case LIST_AUDIT_LOG:            return listAuditLog(request, session);
            case VERIFY_AUDIT_CHAIN:        return verifyChain(session);
            case CREATE_STAFF_USER:         return createStaffUser(request, session);
            case DEACTIVATE_USER:           return deactivateUser(request, session);
            case ADD_TEAM:                  return addTeam(request, session);
            case DEACTIVATE_TEAM:           return deactivateTeam(request, session);
            case ADD_DEPARTMENT:            return addDepartment(request, session);
            case DEACTIVATE_DEPARTMENT:     return deactivateDepartment(request, session);
            case ADD_LOCATION:              return addLocation(request, session);
            case DEACTIVATE_LOCATION:       return deactivateLocation(request, session);
            default:
                return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                        "Unsupported operation: " + op);
        }
    }

    private Response listUsers(Session session) {
        // ADMIN: full user management (e.g. admin panel users tab).
        // COORDINATOR + TEAM_LEADER: read-only need for assignee
        // pickers when creating recovery tasks. Password hashes are
        // stripped before sending regardless of caller role.
        authz.requireAnyRole(session, UserRole.ADMIN,
                UserRole.COORDINATOR, UserRole.TEAM_LEADER);
        List<User> users = context.getUserRepository().findAll();
        // Strip password hashes before returning to client
        for (User u : users) {
            u.setPasswordHash(null);
        }
        return ok(new ArrayList<>(users));
    }

    private Response listDepartments(Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN,
                UserRole.TEAM_LEADER);
        List<Department> list = context.getDepartmentRepository().findAll();
        return ok(new ArrayList<>(list));
    }

    private Response listTeams(Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        List<ResponseTeam> list = context.getResponseTeamRepository().findAll();
        return ok(new ArrayList<>(list));
    }

    private Response listLocations(Session session) {
        authz.requireAuthenticated(session);
        List<Location> list = context.getLocationRepository().findAll();
        return ok(new ArrayList<>(list));
    }

    private Response listAuditLog(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        Number limit = request.get("limit");
        List<AuditLog> entries = (limit == null)
                ? context.getAuditService().getRecentEntries(100)
                : context.getAuditService().getRecentEntries(limit.intValue());
        return ok(new ArrayList<>(entries));
    }

    private Response verifyChain(Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        boolean valid = context.getAuditService().verifyChain();
        context.getAuditService().logAction(session.getUserPk(),
                "VERIFY_AUDIT_CHAIN", null, null,
                "result=" + valid, session.getClientIp(), true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("valid", valid);
        return ok(payload);
    }

    // -------------------------------------------------------------
    // Admin write operations.
    // Every method begins with requireRole(ADMIN) - defence in depth
    // alongside the operation-level dispatch table above.
    // -------------------------------------------------------------

    private Response createStaffUser(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String fullName = request.get("fullName");
        String email = request.get("email");
        String password = request.get("password");
        String roleStr = request.get("role");
        UserRole role;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (Exception e) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Invalid role: " + roleStr);
        }

        // Optional department for agency_rep / team_leader. Resolved
        // by code (display-friendly) into a PK before passing.
        Integer departmentPk = null;
        String deptCode = request.get("departmentCode");
        if (deptCode != null && !deptCode.isBlank()) {
            Department d = context.getDepartmentRepository()
                    .findByCode(deptCode).orElse(null);
            if (d == null) {
                return error(ProtocolConstants.ERR_VALIDATION,
                        "Department not found: " + deptCode);
            }
            departmentPk = d.getDepartmentPk();
        }

        User user = context.getAuthenticationService()
                .createStaffUser(fullName, email, password, role,
                        departmentPk, session.getUserCode(),
                        session.getClientIp());
        Map<String, Object> payload = new HashMap<>();
        payload.put("user", user);
        return ok(payload);
    }

    private Response deactivateUser(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String targetCode = request.get("userCode");
        if (targetCode == null || targetCode.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "userCode is required.");
        }
        // Self-deactivate guard: an admin disabling their own account
        // would lock themselves out on the next operation. Block here.
        if (targetCode.equals(session.getUserCode())) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "You cannot deactivate your own account.");
        }
        context.getAuthenticationService().deactivateUser(
                targetCode, session.getUserCode(), session.getClientIp());
        return ok();
    }

    private Response addTeam(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String teamName = request.get("teamName");
        String departmentCode = request.get("departmentCode");
        Number latNum = request.get("latitude");
        Number lonNum = request.get("longitude");

        if (teamName == null || teamName.trim().length() < 2) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Team name is required (at least 2 characters).");
        }
        if (departmentCode == null || departmentCode.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Department is required.");
        }
        Department dept = context.getDepartmentRepository()
                .findByCode(departmentCode).orElse(null);
        if (dept == null) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Department not found: " + departmentCode);
        }

        ResponseTeam team = new ResponseTeam();
        team.setTeamCode(drs.server.util.IdGenerator.generateTeamCode());
        team.setTeamName(teamName.trim());
        team.setDepartmentPk(dept.getDepartmentPk());
        team.setDepartmentCode(dept.getDepartmentCode());
        team.setDepartmentType(dept.getDepartmentType().name());
        team.setLatitude(latNum == null ? 0.0 : latNum.doubleValue());
        team.setLongitude(lonNum == null ? 0.0 : lonNum.doubleValue());
        team.setAvailability(drs.shared.enums.TeamAvailability.AVAILABLE);
        team.setActive(true);
        team.setCreatedAt(java.time.LocalDateTime.now());

        int newPk = context.getResponseTeamRepository().save(team);
        team.setTeamPk(newPk);

        context.getAuditService().logAction(null, "ADD_TEAM", "Team",
                team.getTeamCode(),
                "By admin " + session.getUserCode(),
                session.getClientIp(), true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("team", team);
        return ok(payload);
    }

    private Response deactivateTeam(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String teamCode = request.get("teamCode");
        if (teamCode == null || teamCode.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "teamCode is required.");
        }
        ResponseTeam team = context.getResponseTeamRepository()
                .findByCode(teamCode).orElse(null);
        if (team == null) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Team not found: " + teamCode);
        }
        context.getResponseTeamRepository()
                .setActive(team.getTeamPk(), false);
        context.getAuditService().logAction(null, "DEACTIVATE_TEAM",
                "Team", team.getTeamCode(),
                "By admin " + session.getUserCode(),
                session.getClientIp(), true);
        return ok();
    }

    private Response addDepartment(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String name = request.get("name");
        String typeStr = request.get("departmentType");

        if (name == null || name.trim().length() < 2) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Department name is required (at least 2 characters).");
        }
        drs.shared.enums.DepartmentType type;
        try {
            type = drs.shared.enums.DepartmentType.valueOf(typeStr);
        } catch (Exception e) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Invalid department type: " + typeStr);
        }

        Department dept = new Department();
        dept.setDepartmentCode(
                drs.server.util.IdGenerator.generateDepartmentCode());
        dept.setName(name.trim());
        dept.setDepartmentType(type);
        dept.setActive(true);
        dept.setCreatedAt(java.time.LocalDateTime.now());

        int newPk = context.getDepartmentRepository().save(dept);
        dept.setDepartmentPk(newPk);

        context.getAuditService().logAction(null, "ADD_DEPARTMENT",
                "Department", dept.getDepartmentCode(),
                "By admin " + session.getUserCode(),
                session.getClientIp(), true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("department", dept);
        return ok(payload);
    }

    private Response deactivateDepartment(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String deptCode = request.get("departmentCode");
        if (deptCode == null || deptCode.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "departmentCode is required.");
        }
        Department dept = context.getDepartmentRepository()
                .findByCode(deptCode).orElse(null);
        if (dept == null) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Department not found: " + deptCode);
        }
        context.getDepartmentRepository()
                .setActive(dept.getDepartmentPk(), false);
        context.getAuditService().logAction(null, "DEACTIVATE_DEPARTMENT",
                "Department", dept.getDepartmentCode(),
                "By admin " + session.getUserCode(),
                session.getClientIp(), true);
        return ok();
    }

    private Response addLocation(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String suburb = request.get("suburb");
        String state = request.get("state");
        String postcode = request.get("postcode");
        String riskZone = request.get("riskZone");
        Number latNum = request.get("latitude");
        Number lonNum = request.get("longitude");

        if (suburb == null || suburb.trim().length() < 2) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Suburb is required.");
        }
        if (state == null || state.trim().length() < 2) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "State is required.");
        }
        if (postcode == null || postcode.trim().length() < 3) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Postcode is required.");
        }
        if (latNum == null || lonNum == null) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Latitude and longitude are required.");
        }

        Location location = new Location();
        location.setLocationCode(
                drs.server.util.IdGenerator.generateLocationCode());
        location.setSuburb(suburb.trim());
        location.setState(state.trim());
        location.setPostcode(postcode.trim());
        location.setRiskZone(riskZone);
        location.setLatitude(latNum.doubleValue());
        location.setLongitude(lonNum.doubleValue());
        location.setDisplayName(suburb.trim() + " "
                + state.trim() + " " + postcode.trim());
        location.setActive(true);
        location.setCreatedAt(java.time.LocalDateTime.now());

        int newPk = context.getLocationRepository().save(location);
        location.setLocationPk(newPk);

        context.getAuditService().logAction(null, "ADD_LOCATION",
                "Location", location.getLocationCode(),
                "By admin " + session.getUserCode(),
                session.getClientIp(), true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("location", location);
        return ok(payload);
    }

    private Response deactivateLocation(Request request, Session session) {
        authz.requireRole(session, UserRole.ADMIN);
        String code = request.get("locationCode");
        if (code == null || code.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "locationCode is required.");
        }
        Location location = context.getLocationRepository()
                .findByCode(code).orElse(null);
        if (location == null) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "Location not found: " + code);
        }
        context.getLocationRepository()
                .setActive(location.getLocationPk(), false);
        context.getAuditService().logAction(null, "DEACTIVATE_LOCATION",
                "Location", location.getLocationCode(),
                "By admin " + session.getUserCode(),
                session.getClientIp(), true);
        return ok();
    }
}
