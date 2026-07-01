package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.enums.DamageLevel;
import drs.shared.enums.InfrastructureStatus;
import drs.shared.enums.RecoveryTaskStatus;
import drs.shared.enums.RecoveryTaskType;
import drs.shared.enums.UserRole;
import drs.shared.exception.ValidationException;
import drs.shared.model.DamageAssessment;
import drs.shared.model.RecoveryTask;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles damage assessment + recovery task operations (Feature 2).
  
 */
public class DamageRecoveryRequestHandler extends AbstractRequestHandler {

    public DamageRecoveryRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        switch (op) {
            case RECORD_DAMAGE_ASSESSMENT:     return recordDamage(request, session);
            case LIST_DAMAGE_ASSESSMENTS:      return listDamage(request, session);
            case CREATE_RECOVERY_TASK:         return createTask(request, session);
            case UPDATE_RECOVERY_TASK_STATUS:  return updateTask(request, session);
            case LIST_RECOVERY_TASKS:          return listTasks(request, session);
            case LIST_MY_RECOVERY_TASKS:       return listMyTasks(session);
            default:
                return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                        "Unsupported operation: " + op);
        }
    }

    private Response recordDamage(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR,
                UserRole.TEAM_LEADER, UserRole.ADMIN);
        String code = request.get("incidentCode");
        DamageLevel building = DamageLevel.valueOf(
                (String) request.get("buildingDamageLevel"));
        InfrastructureStatus road = InfrastructureStatus.valueOf(
                (String) request.get("roadStatus"));
        InfrastructureStatus power = InfrastructureStatus.valueOf(
                (String) request.get("powerStatus"));
        InfrastructureStatus water = InfrastructureStatus.valueOf(
                (String) request.get("waterStatus"));
        Number casualties = request.get("casualtyEstimate");
        String notes = request.get("notes");

        DamageAssessment a = context.getDamageAssessmentService()
                .recordAssessment(code, session.getUserCode(),
                        building, road, power, water,
                        casualties == null ? 0 : casualties.intValue(),
                        notes, session.getClientIp());
        return ok(a);
    }

    private Response listDamage(Request request, Session session) {
        authz.requireAuthenticated(session);
        String code = request.get("incidentCode");
        List<DamageAssessment> list = context.getDamageAssessmentService()
                .listAssessmentsForIncident(code);
        return ok(new ArrayList<>(list));
    }

    private Response createTask(Request request, Session session) {
        // Team leaders own recovery-task creation for incidents
        // they're responding to; coordinators can also create them
        // during planning; admins for system seeding.
        authz.requireAnyRole(session, UserRole.COORDINATOR,
                UserRole.ADMIN, UserRole.TEAM_LEADER);
        String incidentCode = request.get("incidentCode");
        String departmentCode = request.get("departmentCode");
        RecoveryTaskType type = RecoveryTaskType.valueOf(
                (String) request.get("taskType"));
        String description = request.get("description");
        RecoveryTask task = context.getRecoveryTaskService().createTask(
                incidentCode, departmentCode, type, description,
                session.getUserCode(), session.getClientIp());

        // Optional immediate assignee
        String assigneeCode = request.get("assigneeUserCode");
        if (assigneeCode != null && !assigneeCode.isEmpty()) {
            context.getRecoveryTaskService().transitionTask(
                    task.getTaskCode(), RecoveryTaskStatus.ASSIGNED,
                    null, assigneeCode, session.getUserCode(),
                    session.getClientIp());
            task = context.getRecoveryTaskRepository()
                    .findByCode(task.getTaskCode())
                    .orElse(task);
        }
        return ok(task);
    }

    private Response updateTask(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR,
                UserRole.TEAM_LEADER, UserRole.ADMIN);
        String taskCode = request.get("taskCode");
        RecoveryTaskStatus newStatus = RecoveryTaskStatus.valueOf(
                (String) request.get("newStatus"));
        String blockedReason = request.get("blockedReason");
        String newAssigneeCode = request.get("newAssigneeCode");

        boolean coordOrAdmin = session.getRole() == UserRole.COORDINATOR
                || session.getRole() == UserRole.ADMIN;
        RecoveryTask existing = context.getRecoveryTaskRepository()
                .findByCode(taskCode)
                .orElseThrow(() -> new ValidationException(
                        "Task not found: " + taskCode));
        context.getRecoveryTaskService().requirePermissionToUpdate(
                existing, session.getUserCode(), coordOrAdmin);

        context.getRecoveryTaskService().transitionTask(taskCode, newStatus,
                blockedReason, newAssigneeCode, session.getUserCode(),
                session.getClientIp());
        return ok();
    }

    private Response listTasks(Request request, Session session) {
        authz.requireAuthenticated(session);
        String code = request.get("incidentCode");
        List<RecoveryTask> list = context.getRecoveryTaskService()
                .listTasksForIncident(code);
        return ok(new ArrayList<>(list));
    }

    private Response listMyTasks(Session session) {
        authz.requireAnyRole(session, UserRole.TEAM_LEADER, UserRole.ADMIN);
        List<RecoveryTask> list = context.getRecoveryTaskService()
                .listMyTasks(session.getUserCode());
        return ok(new ArrayList<>(list));
    }
}
