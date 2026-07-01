package drs.shared.util;

import drs.shared.exception.DrsException;
import drs.shared.model.AuditLog;

import java.security.MessageDigest;
import java.util.List;

/**
 * Tamper-evident SHA-256 hash chain for audit log entries.
 *
 * Each audit log entry's {@code current_hash} is SHA-256 of
 * ({@code prev_hash} + "|" + content). The first row in the chain uses
 * {@link DrsConstants#AUDIT_GENESIS_HASH} as its prev_hash. Changing any
 * row breaks the link to every subsequent row.
  
 */
public final class HashChain {

    private HashChain() {
        // Static utility
    }

    /*   * Compute the current_hash for an audit log entry, given the previous
     * row's current_hash and a serialised content string.
         * @param prevHash previous row's current_hash (or
     *                {@link DrsConstants#AUDIT_GENESIS_HASH} for the first
     *                row)
     * @param content  pipe-delimited serialised content
     * @return hex-encoded SHA-256 digest (64 chars)
     */
    public static String computeHash(String prevHash, String content) {
        String input = (prevHash == null ? DrsConstants.AUDIT_GENESIS_HASH : prevHash)
                + "|" + content;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new DrsException("SHA-256 hashing failed", e);
        }
    }

    /*   * Verify a list of audit log entries forms a valid hash chain.
         * @param entries entries in audit_pk ASC order
     * @return true if every row's current_hash matches the recomputed
     *        hash AND every row's prev_hash matches the previous row's
     *        current_hash. Empty list is trivially valid.
     */
    public static boolean verifyChain(List<AuditLog> entries) {
        if (entries == null || entries.isEmpty()) {
            return true;
        }
        String expectedPrev = DrsConstants.AUDIT_GENESIS_HASH;
        for (AuditLog entry : entries) {
            String storedPrev = entry.getPrevHash() == null
                    ? DrsConstants.AUDIT_GENESIS_HASH
                    : entry.getPrevHash();
            if (!storedPrev.equals(expectedPrev)) {
                return false;
            }
            String recomputed = computeHash(entry.getPrevHash(), buildContent(entry));
            if (!recomputed.equals(entry.getCurrentHash())) {
                return false;
            }
            expectedPrev = entry.getCurrentHash();
        }
        return true;
    }

    /*   * Build the canonical content string for an audit log entry, used as
     * input to {@link #computeHash(String, String)}.
         * @param entry the audit row
     * @return pipe-delimited canonical content
     */
    public static String buildContent(AuditLog entry) {
        return entry.getAuditCode() + "|"
             + (entry.getUserPk() == null ? "null" : entry.getUserPk()) + "|"
             + entry.getAction() + "|"
             + (entry.getEntityType() == null ? "null" : entry.getEntityType()) + "|"
             + (entry.getEntityCode() == null ? "null" : entry.getEntityCode()) + "|"
             + entry.isSuccess() + "|"
             + DateTimeUtil.format(entry.getCreatedAt());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
