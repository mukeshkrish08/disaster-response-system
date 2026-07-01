package drs.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A tamper-evident audit log entry. Each row stores {@code prev_hash}
 * (the previous row's {@code current_hash}) and {@code current_hash}
 * (SHA-256 of prev_hash + content), forming an append-only hash chain.
 *
 * The {@code details} field is plaintext in Java but stored AES-encrypted
 * in the database column {@code details_encrypted}.
  
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private int auditPk;
    private String auditCode;
    private Integer userPk;          // nullable (e.g. failed login)
    private String userCode;         // hydrated for display (nullable)
    private String action;           // e.g. "LOGIN_SUCCESS", "REPORT_INCIDENT"
    private String entityType;       // e.g. "Incident" (nullable)
    private String entityCode;       // e.g. "INC-2026-0001" (nullable)
    private String details;          // plaintext in Java
    private String clientIp;         // nullable
    private boolean success;
    private String prevHash;         // 64 hex chars, or null for first row
    private String currentHash;      // 64 hex chars
    private LocalDateTime createdAt;

    public AuditLog() {
        // No-arg for JDBC/Serialization
    }

    public int getAuditPk() { return auditPk; }
    public void setAuditPk(int auditPk) { this.auditPk = auditPk; }

    public String getAuditCode() { return auditCode; }
    public void setAuditCode(String auditCode) { this.auditCode = auditCode; }

    public Integer getUserPk() { return userPk; }
    public void setUserPk(Integer userPk) { this.userPk = userPk; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityCode() { return entityCode; }
    public void setEntityCode(String entityCode) { this.entityCode = entityCode; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog)) return false;
        return auditPk == ((AuditLog) o).auditPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(auditPk);
    }

    @Override
    public String toString() {
        return "AuditLog{" + auditCode + ", " + action
             + ", success=" + success + "}";
    }
}
