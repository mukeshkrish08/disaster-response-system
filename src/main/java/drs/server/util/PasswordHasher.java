package drs.server.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing utility.
 *
 * BCrypt is salted, slow (configurable cost factor), and one-way. The
 * stored hash includes the salt so {@code verify()} doesn't need an
 * external salt argument.
  
 */
public final class PasswordHasher {

    /** Cost factor - higher = slower = more resistant to brute force. */
    private static final int COST = 12;

    private PasswordHasher() {
        // Static utility
    }

    /*   * Hash a plaintext password with a random salt at cost factor 12.
         * @param plaintext the plain password
     * @return BCrypt hash including the embedded salt
     */
    public static String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(COST));
    }

    /*   * Verify a plaintext password against a stored BCrypt hash.
         * @param plaintext   the plain candidate
     * @param storedHash  the previously-stored BCrypt hash
     * @return true if the plaintext matches the hash
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, storedHash);
        } catch (IllegalArgumentException ex) {
            // Malformed stored hash
            return false;
        }
    }
}
