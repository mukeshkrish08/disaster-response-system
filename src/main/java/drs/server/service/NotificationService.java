package drs.server.service;

import drs.server.repository.DatabaseConnection;
import drs.server.repository.NotificationRepository;
import drs.server.repository.UserRepository;
import drs.server.util.IdGenerator;
import drs.shared.enums.NotificationStatus;
import drs.shared.exception.DataAccessException;
import drs.shared.exception.ValidationException;
import drs.shared.model.Notification;
import drs.shared.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Inter-agency notifications. Coordinators broadcast to agency reps,
 * agency reps acknowledge.
  
 */
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /*   * Send a notification about an incident to a specific user.
         * @param incidentPk        target incident pk
     * @param recipientUserCode user to notify
     * @param message           short message
     * @param senderUserCode    who is sending
     * @param clientIp          remote address
     * @return persisted Notification
     */
    public Notification sendNotification(int incidentPk,
                                         String recipientUserCode,
                                         String message,
                                         String senderUserCode,
                                         String clientIp) {
        if (message == null || message.trim().isEmpty()) {
            throw new ValidationException("Notification message cannot be empty.");
        }
        User recipient = userRepository.findByCode(recipientUserCode)
                .orElseThrow(() -> new ValidationException(
                        "Recipient not found: " + recipientUserCode));
        User sender = userRepository.findByCode(senderUserCode)
                .orElseThrow(() -> new ValidationException("Sender not found"));

        Notification n = new Notification();
        n.setNotificationCode(IdGenerator.generateNotificationCode());
        n.setIncidentPk(incidentPk);
        n.setRecipientUserPk(recipient.getUserPk());
        n.setMessage(message);
        n.setStatus(NotificationStatus.PENDING);
        n.setCreatedAt(LocalDateTime.now());

        try (Connection c = DatabaseConnection.getConnection()) {
            notificationRepository.save(c, n);
        } catch (SQLException e) {
            throw new DataAccessException("sendNotification failed", e);
        }

        auditService.logAction(sender.getUserPk(), "SEND_NOTIFICATION",
                "Notification", n.getNotificationCode(),
                "to " + recipientUserCode, clientIp, true);
        return n;
    }

    public List<Notification> getNotificationsForUser(String userCode) {
        User user = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException(
                        "User not found: " + userCode));
        return notificationRepository.findByRecipientPk(user.getUserPk());
    }

    /*   * Mark a notification as acknowledged.
     */
    public void acknowledgeNotification(String notificationCode,
                                        String userCode, String clientIp) {
        Notification n = notificationRepository.findByCode(notificationCode)
                .orElseThrow(() -> new ValidationException(
                        "Notification not found: " + notificationCode));
        User user = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ValidationException("User not found"));
        if (n.getRecipientUserPk() != user.getUserPk()) {
            throw new ValidationException(
                    "You may only acknowledge your own notifications.");
        }
        notificationRepository.updateStatus(n.getNotificationPk(),
                NotificationStatus.ACKNOWLEDGED, LocalDateTime.now());
        auditService.logAction(user.getUserPk(), "ACK_NOTIFICATION",
                "Notification", notificationCode, null, clientIp, true);
    }
}
