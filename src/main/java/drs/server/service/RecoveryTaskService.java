package drs.server.service;

import drs.server.repository.DepartmentRepository;
import drs.server.repository.IncidentRepository;
import drs.server.repository.RecoveryTaskRepository;
import drs.server.repository.UserRepository;
import drs.server.util.IdGenerator;
import drs.shared.enums.RecoveryTaskStatus;
import drs.shared.enums.RecoveryTaskType;
import drs.shared.exception.AuthorizationException;
import drs.shared.exception.InvalidStateTransitionException;
import drs.shared.exception.ValidationException;
import drs.shared.model.Department;
import drs.shared.model.Incident;
import drs.shared.model.RecoveryTask;
import drs.shared.model.User;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages recovery tasks for incidents during the recovery phase. Owns
 * its own state machine, parallel to (but independent of) the incident
 * lifecycle.
 *
 * Valid task transitions:
 *  OPEN -> ASSIGNED -> IN_PROGRESS -> COMPLETED
 *  any active state -> BLOCKED (requires reason)
 *  any active state -> CANCELLED
  
 */
public class RecoveryTaskService {

    /** Allowed transitions per current status. */
    private static final Map<RecoveryTaskStatus, Set<RecoveryTaskStatus>> TRANSITIONS;
    static {
        TRANSITIONS = new EnumMap<>(RecoveryTaskStatus.class);
        TRANSITIONS.put(RecoveryTaskStatus.OPEN,
                EnumSet.of(RecoveryTaskStatus.ASSIGNED,
                           RecoveryTaskStatus.CANCELLED,
                           RecoveryTaskStatus.BLOCKED));
        TRANSITIONS.put(RecoveryTaskStatus.ASSIGNED,
                EnumSet.of(RecoveryTaskStatus.IN_PROGRESS,
                           RecoveryTaskStatus.BLOCKED,
                           RecoveryTaskStatus.CANCELLED));
        TRANSITIONS.put(RecoveryTaskStatus.IN_PROGRESS,
                EnumSet.of(RecoveryTaskStatus.COMPLETED,
                           RecoveryTaskStatus.BLOCKED,
                           RecoveryTaskStatus.CANCELLED));
        TRANSITIONS.put(RecoveryTaskStatus.BLOCKED,
                EnumSet.of(RecoveryTaskStatus.IN_PROGRESS,
                           RecoveryTaskStatus.CANCELLED));
        TRANSITIONS.put(RecoveryTaskStatus.COMPLETED,
                EnumSet.noneOf(RecoveryTaskStatus.class));
        TRANSITIONS.put(RecoveryTaskStatus.CANCELLED,
                EnumSet.noneOf(RecoveryTaskStatus.class));
    }

    private final RecoveryTaskRepository taskRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public RecoveryTaskService(RecoveryTaskRepository taskRepository,
                               IncidentRepository incidentRepository,
                               UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               AuditService auditService) {
        this.taskRepository = taskRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    /*   * Create a new recovery task (state OPEN).
         * @param incidentCode       parent incident
     * @param departmentCode     owning department
     * @param taskType           category
     * @param description        non-empty description
     * @param createdByUserCode  creating coordinator
     * @param clientIp           remote address
     * @return the persisted RecoveryTask
     */
    public RecoveryTask createTask(String incidentCode, String departmentCode,
                                   RecoveryTaskType taskType,
                                   String description,
                                   String createdByUserCode, String clientIp) {
        if (taskType == null) {
            throw new ValidationException("Task type is required.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Description is required.");
        }
        if (description.length() > 500) {
            throw new ValidationException(
                    "Description must be 500 characters or fewer.");
        }
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        Department dept = departmentRepository.findByCode(departmentCode)
                .orElseThrow(() -> new ValidationException(
                        "Department not found: " + departmentCode));
        User creator = userRepository.findByCode(createdByUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        RecoveryTask task = new RecoveryTask();
        task.setTaskCode(IdGenerator.generateTaskCode());
        task.setIncidentPk(incident.getIncidentPk());
        task.setDepartmentPk(dept.getDepartmentPk());
        task.setTaskType(taskType);
        task.setDescription(description.trim());
        task.setStatus(RecoveryTaskStatus.OPEN);
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        auditService.logAction(creator.getUserPk(), "CREATE_RECOVERY_TASK",
                "RecoveryTask", task.getTaskCode(),
                taskType + " for " + incident.getIncidentCode(),
                clientIp, true);
        return task;
    }

    /*   * Assign a task to a user. Moves OPEN -> ASSIGNED.
     */
    public void assignTask(String taskCode, String assigneeUserCode,
                           String assignerUserCode, String clientIp) {
        User assignee = userRepository.findByCode(assigneeUserCode)
                .orElseThrow(() -> new ValidationException(
                        "Assignee not found: " + assigneeUserCode));
        transitionTask(taskCode, RecoveryTaskStatus.ASSIGNED,
                null, assigneeUserCode, assignerUserCode, clientIp);
        // assignee variable verified above; silence unused warning by
        // referencing it
        if (assignee.getUserPk() <= 0) {
            throw new ValidationException("Invalid assignee");
        }
    }

    /*   * Transition a task to a new status. Validates against the state
     * machine and authorisation rules.
         * @param taskCode        target task
     * @param newStatus       desired next status
     * @param blockedReason   required iff newStatus == BLOCKED
     * @param newAssigneeCode optional new assignee (for ASSIGNED moves)
     * @param actorUserCode   user performing the action
     * @param clientIp        remote address
     */
    public void transitionTask(String taskCode, RecoveryTaskStatus newStatus,
                               String blockedReason, String newAssigneeCode,
                               String actorUserCode, String clientIp) {
        if (newStatus == null) {
            throw new ValidationException("Target status is required.");
        }
        if (newStatus == RecoveryTaskStatus.BLOCKED
                && (blockedReason == null || blockedReason.trim().isEmpty())) {
            throw new ValidationException(
                    "A reason is required when blocking a task.");
        }
        RecoveryTask task = taskRepository.findByCode(taskCode)
                .orElseThrow(() -> new ValidationException(
                        "Task not found: " + taskCode));
        Set<RecoveryTaskStatus> allowed = TRANSITIONS.get(task.getStatus());
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move task from " + task.getStatus().displayName()
                            + " to " + newStatus.displayName() + ".");
        }
        User actor = userRepository.findByCode(actorUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Permission: a team leader may only update their own tasks
        // (coordinator and admin are allowed via handler-level role check)
        if (task.getAssignedToUserPk() != null
                && task.getStatus() != RecoveryTaskStatus.OPEN
                && task.getAssignedToUserPk() != actor.getUserPk()) {
            // Final permission check delegated to handler - here we just
            // record that assigning to someone else moves through.
            // (Coordinators bypass this via the handler.)
        }

        Integer assigneePk = task.getAssignedToUserPk();
        if (newAssigneeCode != null && !newAssigneeCode.isEmpty()) {
            User assignee = userRepository.findByCode(newAssigneeCode)
                    .orElseThrow(() -> new ValidationException(
                            "Assignee not found: " + newAssigneeCode));
            assigneePk = assignee.getUserPk();
        }
        if (newStatus == RecoveryTaskStatus.ASSIGNED && assigneePk == null) {
            throw new ValidationException(
                    "An assignee is required when moving to ASSIGNED.");
        }

        taskRepository.updateStatus(task.getTaskPk(), newStatus,
                newStatus == RecoveryTaskStatus.BLOCKED ? blockedReason : null,
                assigneePk, LocalDateTime.now());

        auditService.logAction(actor.getUserPk(),
                "UPDATE_RECOVERY_TASK", "RecoveryTask", task.getTaskCode(),
                task.getStatus() + " -> " + newStatus
                        + (blockedReason != null
                                ? " (" + blockedReason + ")" : ""),
                clientIp, true);
    }

    public List<RecoveryTask> listTasksForIncident(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        return taskRepository.findByIncidentPk(incident.getIncidentPk());
    }

    public List<RecoveryTask> listMyTasks(String assigneeUserCode) {
        User user = userRepository.findByCode(assigneeUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));
        return taskRepository.findByAssignedToPk(user.getUserPk());
    }

    /*   * Hard authorisation: verify the actor is allowed to update a task.
     * Coordinator/Admin can update any; team leader can update only
     * their own assigned tasks. Called by handler before transitionTask.
         * @param task              the task
     * @param actorUserCode     acting user
     * @param coordinatorOrAdmin true if actor has elevated role
     */
    public void requirePermissionToUpdate(RecoveryTask task,
                                          String actorUserCode,
                                          boolean coordinatorOrAdmin) {
        if (coordinatorOrAdmin) {
            return;
        }
        User actor = userRepository.findByCode(actorUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));
        if (task.getAssignedToUserPk() == null
                || task.getAssignedToUserPk() != actor.getUserPk()) {
            throw new AuthorizationException(
                    "Only the assigned team leader can update this task.");
        }
    }
}
