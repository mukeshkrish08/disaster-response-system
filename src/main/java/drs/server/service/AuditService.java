package drs.server.service;

import drs.server.repository.AuditLogRepository;
import drs.server.util.IdGenerator;
import drs.shared.model.AuditLog;
import drs.shared.util.HashChain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Append-only audit log with SHA-256 hash chain (non-repudiation).
 *
 * Each call to {@link #logAction(...)} reads the latest row's
 * current_hash, computes the new row's hash from it plus the row's
 * canonical content, and writes a new row. {@link #verifyChain()}
 * walks the chain and returns true iff every link is intact.
 *
 * Synchronisation: the read-then-write sequence must be atomic across
 * threads - multiple ClientHandler threads can call logAction
 * concurrently. We synchronise on the service instance.
  
 */
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /*   * Record an audited action.
         * @param userPk      user pk (nullable for failed login)
     * @param action      action name (e.g. "LOGIN_SUCCESS")
     * @param entityType  entity type name (e.g. "Incident", nullable)
     * @param entityCode  entity readable code (nullable)
     * @param details     plaintext details (will be AES-encrypted in DB)
     * @param clientIp    socket remote address (nullable)
     * @param success     whether the action succeeded
     * @return the persisted AuditLog
     */
    public synchronized AuditLog logAction(Integer userPk, String action,
                                           String entityType, String entityCode,
                                           String details, String clientIp,
                                           boolean success) {
        Optional<AuditLog> latest = auditLogRepository.findLatest();
        String prevHash = latest.map(AuditLog::getCurrentHash).orElse(null);

        AuditLog entry = new AuditLog();
        entry.setAuditCode(IdGenerator.generateAuditCode());
        entry.setUserPk(userPk);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityCode(entityCode);
        entry.setDetails(details);
        entry.setClientIp(clientIp);
        entry.setSuccess(success);
        entry.setPrevHash(prevHash);
        entry.setCreatedAt(LocalDateTime.now());

        String content = HashChain.buildContent(entry);
        entry.setCurrentHash(HashChain.computeHash(prevHash, content));

        auditLogRepository.save(entry);
        return entry;
    }

    public List<AuditLog> getRecentEntries(int limit) {
        return auditLogRepository.findRecent(limit);
    }

    public List<AuditLog> getAllEntries() {
        return auditLogRepository.findAll();
    }

    /*   * Verify the entire audit chain is intact.
         * @return true if every row's hash is correct AND links to the
     *        previous row's hash
     */
    public boolean verifyChain() {
        return HashChain.verifyChain(auditLogRepository.findAll());
    }
}
