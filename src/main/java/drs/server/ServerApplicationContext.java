package drs.server;

import drs.server.repository.AuditLogRepository;
import drs.server.repository.DamageAssessmentRepository;
import drs.server.repository.DepartmentRepository;
import drs.server.repository.IncidentAssignmentRepository;
import drs.server.repository.IncidentRepository;
import drs.server.repository.IncidentUpdateRepository;
import drs.server.repository.LocationRepository;
import drs.server.repository.NotificationRepository;
import drs.server.repository.RecoveryTaskRepository;
import drs.server.repository.ResourceAllocationRepository;
import drs.server.repository.ResourceRepository;
import drs.server.repository.ResponseTeamRepository;
import drs.server.repository.UserRepository;
import drs.server.service.AssignmentService;
import drs.server.service.AuditService;
import drs.server.service.AuthenticationService;
import drs.server.service.DamageAssessmentService;
import drs.server.service.GeoDispatchService;
import drs.server.service.IncidentService;
import drs.server.service.NotificationService;
import drs.server.service.PriorityService;
import drs.server.service.RecoveryTaskService;
import drs.server.service.ResourceService;

/**
 * Dependency-injection root for the server. Owns one instance of every
 * repository and service, shared across all ClientHandler threads.
 *
 * Services and repositories are stateless beyond their construction-time
 * collaborators, so a single instance per process is safe.
 *
 * Constructed once by {@link DrsServerApplication}.
  
 */
public class ServerApplicationContext {

    // -- Repositories ------------------------------------------------------
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final ResponseTeamRepository responseTeamRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentAssignmentRepository assignmentRepository;
    private final IncidentUpdateRepository updateRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final RecoveryTaskRepository recoveryTaskRepository;

    // -- Services ----------------------------------------------------------
    private final AuditService auditService;
    private final AuthenticationService authenticationService;
    private final PriorityService priorityService;
    private final IncidentService incidentService;
    private final GeoDispatchService geoDispatchService;
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;
    private final ResourceService resourceService;
    private final DamageAssessmentService damageAssessmentService;
    private final RecoveryTaskService recoveryTaskService;

    // -- Infra -------------------------------------------------------------
    private final SessionManager sessionManager;
    private final AuthorizationService authorizationService;

    public ServerApplicationContext(int sessionTimeoutMins) {
        // Repositories
        this.userRepository = new UserRepository();
        this.departmentRepository = new DepartmentRepository();
        this.locationRepository = new LocationRepository();
        this.responseTeamRepository = new ResponseTeamRepository();
        this.incidentRepository = new IncidentRepository();
        this.assignmentRepository = new IncidentAssignmentRepository();
        this.updateRepository = new IncidentUpdateRepository();
        this.notificationRepository = new NotificationRepository();
        this.auditLogRepository = new AuditLogRepository();
        this.resourceRepository = new ResourceRepository();
        this.allocationRepository = new ResourceAllocationRepository();
        this.damageAssessmentRepository = new DamageAssessmentRepository();
        this.recoveryTaskRepository = new RecoveryTaskRepository();

        // Infra
        this.sessionManager = new SessionManager(sessionTimeoutMins);
        this.authorizationService = new AuthorizationService();

        // Services (order matters - some depend on others)
        this.auditService = new AuditService(auditLogRepository);
        this.authenticationService = new AuthenticationService(
                userRepository, auditService);
        this.priorityService = new PriorityService();
        this.incidentService = new IncidentService(
                incidentRepository, updateRepository, userRepository,
                locationRepository, priorityService, auditService);
        this.geoDispatchService = new GeoDispatchService(responseTeamRepository);
        this.assignmentService = new AssignmentService(
                assignmentRepository, updateRepository, responseTeamRepository,
                incidentRepository, userRepository, notificationRepository,
                auditService);
        this.notificationService = new NotificationService(
                notificationRepository, userRepository, auditService);
        this.resourceService = new ResourceService(
                resourceRepository, allocationRepository, incidentRepository,
                userRepository, locationRepository, auditService);
        this.damageAssessmentService = new DamageAssessmentService(
                damageAssessmentRepository, incidentRepository, userRepository,
                auditService);
        this.recoveryTaskService = new RecoveryTaskService(
                recoveryTaskRepository, incidentRepository, userRepository,
                departmentRepository, auditService);
    }

    public UserRepository getUserRepository()                 { return userRepository; }
    public DepartmentRepository getDepartmentRepository()     { return departmentRepository; }
    public LocationRepository getLocationRepository()         { return locationRepository; }
    public ResponseTeamRepository getResponseTeamRepository() { return responseTeamRepository; }
    public IncidentRepository getIncidentRepository()         { return incidentRepository; }
    public IncidentAssignmentRepository getAssignmentRepository() { return assignmentRepository; }
    public IncidentUpdateRepository getUpdateRepository()     { return updateRepository; }
    public NotificationRepository getNotificationRepository() { return notificationRepository; }
    public AuditLogRepository getAuditLogRepository()         { return auditLogRepository; }
    public ResourceRepository getResourceRepository()         { return resourceRepository; }
    public ResourceAllocationRepository getAllocationRepository() { return allocationRepository; }
    public DamageAssessmentRepository getDamageAssessmentRepository() { return damageAssessmentRepository; }
    public RecoveryTaskRepository getRecoveryTaskRepository() { return recoveryTaskRepository; }

    public AuditService getAuditService()                     { return auditService; }
    public AuthenticationService getAuthenticationService()   { return authenticationService; }
    public PriorityService getPriorityService()               { return priorityService; }
    public IncidentService getIncidentService()               { return incidentService; }
    public GeoDispatchService getGeoDispatchService()         { return geoDispatchService; }
    public AssignmentService getAssignmentService()           { return assignmentService; }
    public NotificationService getNotificationService()       { return notificationService; }
    public ResourceService getResourceService()               { return resourceService; }
    public DamageAssessmentService getDamageAssessmentService() { return damageAssessmentService; }
    public RecoveryTaskService getRecoveryTaskService()       { return recoveryTaskService; }

    public SessionManager getSessionManager()                 { return sessionManager; }
    public AuthorizationService getAuthorizationService()     { return authorizationService; }
}
