package drs.shared.model;

import drs.shared.enums.NotificationStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An inter-agency notification about an incident. Routed to a specific
 * user (typically an Agency Representative).
  
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private int notificationPk;
    private String notificationCode;
    private int incidentPk;
    private String incidentCode;        // hydrated for display
    private int recipientUserPk;
    private String recipientUserCode;   // hydrated for display
    private String message;
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;

    public Notification() {
        // No-arg for JDBC/Serialization
        this.status = NotificationStatus.PENDING;
    }

    public int getNotificationPk() { return notificationPk; }
    public void setNotificationPk(int notificationPk) { this.notificationPk = notificationPk; }

    public String getNotificationCode() { return notificationCode; }
    public void setNotificationCode(String notificationCode) { this.notificationCode = notificationCode; }

    public int getIncidentPk() { return incidentPk; }
    public void setIncidentPk(int incidentPk) { this.incidentPk = incidentPk; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public int getRecipientUserPk() { return recipientUserPk; }
    public void setRecipientUserPk(int recipientUserPk) { this.recipientUserPk = recipientUserPk; }

    public String getRecipientUserCode() { return recipientUserCode; }
    public void setRecipientUserCode(String recipientUserCode) { this.recipientUserCode = recipientUserCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        return notificationPk == ((Notification) o).notificationPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationPk);
    }

    @Override
    public String toString() {
        return "Notification{" + notificationCode + ", " + status + "}";
    }
}
