package drs.server.service;

import drs.server.repository.DatabaseConnection;
import drs.server.repository.IncidentAssignmentRepository;
import drs.server.repository.IncidentRepository;
import drs.server.repository.IncidentUpdateRepository;
import drs.server.repository.NotificationRepository;
import drs.server.repository.ResponseTeamRepository;
import drs.server.repository.UserRepository;
import drs.server.util.IdGenerator;
import drs.shared.enums.AssignmentRole;
import drs.shared.enums.AssignmentStatus;
import drs.shared.enums.IncidentStatus;
import drs.shared.enums.NotificationStatus;
import drs.shared.enums.TeamAvailability;
import drs.shared.exception.DataAccessException;
import drs.shared.exception.ValidationException;
import drs.shared.model.Incident;
import drs.shared.model.IncidentAssignment;
import drs.shared.model.IncidentUpdate;
import drs.shared.model.Notification;
import drs.shared.model.ResponseTeam;
import drs.shared.model.User;
import drs.shared.state.IncidentStateRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Owns assignment of {@link ResponseTeam}s to {@link Incident}s and
 * the operational handoff between coordinator and team leader.
 *
 * {@link #assignTeams} runs inside a JDBC transaction because it does
 * up to six writes that must succeed together (or roll back together).
  
 */
public class AssignmentService {

    private final IncidentAssignmentRepository assignmentRepository;
    private final IncidentUpdateRepository updateRepository;
    private final ResponseTeamRepository teamRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    public AssignmentService(IncidentAssignmentRepository assignmentRepository,
                             IncidentUpdateRepository updateRepository,
                             ResponseTeamRepository teamRepository,
                             IncidentRepository incidentRepository,
                             UserRepository userRepository,
                             NotificationRepository notificationRepository,
                             AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.updateRepository = updateRepository;
        this.teamRepository = teamRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
    }

    /*   * Assign primary and (optional) secondary teams to an incident in a
     * single transaction. On failure everything rolls back.
         * @param incidentCode      the incident
     * @param primaryTeamCode   primary team
     * @param secondaryTeamCode secondary team (nullable)
     * @param primaryDistance   km from team to incident
     * @param secondaryDistance km (only used if secondaryTeamCode != null)
     * @param coordinatorCode   acting coordinator
     * @param clientIp          remote address
     */
    public void assignTeams(String incidentCode, String primaryTeamCode,
                            String secondaryTeamCode,
                            double primaryDistance, double secondaryDistance,
                            String coordinatorCode, String clientIp) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        IncidentStateRegistry.validateTransition(
                incident.getStatus(), IncidentStatus.ASSIGNED);

        ResponseTeam primary = teamRepository.findByCode(primaryTeamCode)
                .orElseThrow(() -> new ValidationException(
                        "Primary team not found: " + primaryTeamCode));
        ResponseTeam secondary = null;
        if (secondaryTeamCode != null && !secondaryTeamCode.isEmpty()) {
            secondary = teamRepository.findByCode(secondaryTeamCode)
                    .orElseThrow(() -> new ValidationException(
                            "Secondary team not found: " + secondaryTeamCode));
        }
        User coordinator = userRepository.findByCode(coordinatorCode)
                .orElseThrow(() -> new ValidationException(
                        "Coordinator user not found"));

        IncidentStatus before = incident.getStatus();
        LocalDateTime now = LocalDateTime.now();

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1. Save primary assignment
                IncidentAssignment primaryAssn = buildAssignment(incident.getIncidentPk(),
                        primary.getTeamPk(), AssignmentRole.PRIMARY,
                        coordinator.getUserPk(), primaryDistance, now);
                assignmentRepository.save(c, primaryAssn);

                // 2. Save secondary (if any)
                if (secondary != null) {
                    IncidentAssignment secAssn = buildAssignment(incident.getIncidentPk(),
                            secondary.getTeamPk(), AssignmentRole.SECONDARY,
                            coordinator.getUserPk(), secondaryDistance, now);
                    assignmentRepository.save(c, secAssn);
                }

                // 3. Record the status transition
                IncidentUpdate u = new IncidentUpdate();
                u.setUpdateCode(IdGenerator.generateUpdateCode());
                u.setIncidentPk(incident.getIncidentPk());
                u.setUpdatedByPk(coordinator.getUserPk());
                u.setStatusBefore(before);
                u.setStatusAfter(IncidentStatus.ASSIGNED);
                u.setComment("Assigned primary=" + primaryTeamCode
                        + (secondary != null ? ", secondary=" + secondaryTeamCode : ""));
                u.setCreatedAt(now);
                updateRepository.save(c, u);

                // 4. Notification to team leader of primary team
                if (primary.getLeaderUserPk() != null) {
                    Notification n = buildNotification(incident.getIncidentPk(),
                            primary.getLeaderUserPk(),
                            "Your team has been assigned to incident "
                                    + incident.getIncidentCode(), now);
                    notificationRepository.save(c, n);
                }

                c.commit();
            } catch (Exception inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("assignTeams transaction failed", e);
        } catch (Exception other) {
            throw new DataAccessException("assignTeams failed", other);
        }

        // 5. Outside the transaction: flip incident status + team availability
        incident.setStatus(IncidentStatus.ASSIGNED);
        incidentRepository.update(incident);
        teamRepository.updateAvailability(primary.getTeamPk(), TeamAvailability.BUSY);
        if (secondary != null) {
            teamRepository.updateAvailability(secondary.getTeamPk(),
                    TeamAvailability.BUSY);
        }

        auditService.logAction(coordinator.getUserPk(), "ASSIGN_TEAMS",
                "Incident", incident.getIncidentCode(),
                "primary=" + primaryTeamCode
                        + (secondary != null ? ", secondary=" + secondaryTeamCode : ""),
                clientIp, true);
    }

    /*   * Team leader starts active response.
     */
    public void startResponse(String incidentCode, String leaderUserCode,
                              String clientIp) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        IncidentStateRegistry.validateTransition(
                incident.getStatus(), IncidentStatus.RESPONDING);
        User leader = userRepository.findByCode(leaderUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        IncidentStatus before = incident.getStatus();
        incident.setStatus(IncidentStatus.RESPONDING);
        incidentRepository.update(incident);

        try (Connection c = DatabaseConnection.getConnection()) {
            IncidentUpdate u = new IncidentUpdate();
            u.setUpdateCode(IdGenerator.generateUpdateCode());
            u.setIncidentPk(incident.getIncidentPk());
            u.setUpdatedByPk(leader.getUserPk());
            u.setStatusBefore(before);
            u.setStatusAfter(IncidentStatus.RESPONDING);
            u.setComment("Team started response");
            u.setCreatedAt(LocalDateTime.now());
            updateRepository.save(c, u);
        } catch (SQLException e) {
            throw new DataAccessException("startResponse failed", e);
        }

        auditService.logAction(leader.getUserPk(), "START_RESPONSE",
                "Incident", incident.getIncidentCode(), null,
                clientIp, true);
    }

    /*   * Team leader completes response (RESPONDING -> RESOLVED).
     * A resolution note (≥10 chars) is mandatory so the coordinator
     * later closing the incident sees what the team accomplished on
     * scene, and the audit chain has the operational record.
     */
    public void completeResponse(String incidentCode, String leaderUserCode,
                                 String resolutionNotes, String clientIp) {
        if (resolutionNotes == null || resolutionNotes.trim().length() < 10) {
            throw new ValidationException(
                    "Resolution note is required (at least 10 characters). "
                            + "Include status of casualties, damage, and "
                            + "scene safety.");
        }
        String notes = resolutionNotes.trim();
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        IncidentStateRegistry.validateTransition(
                incident.getStatus(), IncidentStatus.RESOLVED);
        User leader = userRepository.findByCode(leaderUserCode)
                .orElseThrow(() -> new ValidationException("User not found"));

        IncidentStatus before = incident.getStatus();
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incidentRepository.update(incident);

        try (Connection c = DatabaseConnection.getConnection()) {
            IncidentUpdate u = new IncidentUpdate();
            u.setUpdateCode(IdGenerator.generateUpdateCode());
            u.setIncidentPk(incident.getIncidentPk());
            u.setUpdatedByPk(leader.getUserPk());
            u.setStatusBefore(before);
            u.setStatusAfter(IncidentStatus.RESOLVED);
            u.setComment(notes);
            u.setCreatedAt(LocalDateTime.now());
            updateRepository.save(c, u);
        } catch (SQLException e) {
            throw new DataAccessException("completeResponse failed", e);
        }

        // Free up the assigned teams
        for (IncidentAssignment a : assignmentRepository
                .findByIncidentPk(incident.getIncidentPk())) {
            assignmentRepository.updateStatus(a.getAssignmentPk(),
                    AssignmentStatus.COMPLETED, LocalDateTime.now());
            teamRepository.updateAvailability(a.getTeamPk(),
                    TeamAvailability.AVAILABLE);
        }

        auditService.logAction(leader.getUserPk(), "COMPLETE_RESPONSE",
                "Incident", incident.getIncidentCode(), notes,
                clientIp, true);
    }

    private IncidentAssignment buildAssignment(int incidentPk, int teamPk,
                                               AssignmentRole role,
                                               int coordinatorPk,
                                               double distanceKm,
                                               LocalDateTime when) {
        IncidentAssignment a = new IncidentAssignment();
        a.setAssignmentCode(IdGenerator.generateAssignmentCode());
        a.setIncidentPk(incidentPk);
        a.setTeamPk(teamPk);
        a.setRole(role);
        a.setAssignmentStatus(AssignmentStatus.ACTIVE);
        a.setAssignedByUserPk(coordinatorPk);
        a.setDistanceKm(distanceKm);
        a.setAssignedAt(when);
        a.setStartedAt(when);
        return a;
    }

    private Notification buildNotification(int incidentPk, int recipientPk,
                                           String message, LocalDateTime when) {
        Notification n = new Notification();
        n.setNotificationCode(IdGenerator.generateNotificationCode());
        n.setIncidentPk(incidentPk);
        n.setRecipientUserPk(recipientPk);
        n.setMessage(message);
        n.setStatus(NotificationStatus.PENDING);
        n.setCreatedAt(when);
        return n;
    }
}
