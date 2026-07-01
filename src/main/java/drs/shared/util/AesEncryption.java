package drs.shared.util;

import drs.shared.exception.DrsException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 256-bit symmetric encryption / decryption.
 *
 * Output format: base64(IV || ciphertext || authTag). A fresh 96-bit IV
 * is generated for each call to {@link #encrypt(String)} so the same
 * plaintext does not produce the same ciphertext twice.
 *
 * The key is supplied via {@link #init(SecretKey)} which is called once
 * during server startup. If the application is used without init, a
 * single transient key is generated automatically - only useful for tests.
  
 */
public final class AesEncryption {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    private static SecretKey secretKey;

    private AesEncryption() {
        // Static utility
    }

    /*   * Provide the application-wide AES key. Called once by the server at
     * startup using the key from drs.properties.
         * @param key 32-byte AES key
     */
    public static synchronized void init(SecretKey key) {
        secretKey = key;
    }

    /*   * Build a SecretKey from a 32-byte raw byte array.
         * @param keyBytes raw AES key (must be exactly
     *                {@link DrsConstants#AES_KEY_LENGTH_BYTES} bytes)
     * @return SecretKey for {@link #init(SecretKey)}
     */
    public static SecretKey keyFromBytes(byte[] keyBytes) {
        if (keyBytes == null
                || keyBytes.length != DrsConstants.AES_KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                "AES key must be " + DrsConstants.AES_KEY_LENGTH_BYTES + " bytes");
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /*   * Generate a fresh random AES-256 key.
         * @return SecretKey, base64-encodable via {@link SecretKey#getEncoded()}
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
            kg.init(DrsConstants.AES_KEY_LENGTH_BYTES * 8);
            return kg.generateKey();
        } catch (Exception e) {
            throw new DrsException("Failed to generate AES key", e);
        }
    }

    /*   * Encrypt a plaintext string and return base64(IV || ciphertext || tag).
         * @param plaintext UTF-8 plaintext (nullable; null returns null)
     * @return base64-encoded encrypted blob, or null when input null
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            SecretKey key = ensureKey();
            byte[] iv = new byte[DrsConstants.AES_GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key,
                    new GCMParameterSpec(DrsConstants.AES_GCM_TAG_LENGTH_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes("UTF-8"));

            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new DrsException("AES encryption failed", e);
        }
    }

    /*   * Decrypt a base64-encoded blob produced by {@link #encrypt(String)}.
         * @param ciphertextBase64 base64 input (nullable; null returns null)
     * @return the original plaintext, or null when input null
     */
    public static String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) {
            return null;
        }
        try {
            SecretKey key = ensureKey();
            byte[] full = Base64.getDecoder().decode(ciphertextBase64);
            if (full.length < DrsConstants.AES_GCM_IV_LENGTH_BYTES + 16) {
                throw new DrsException("Ciphertext too short");
            }
            byte[] iv = new byte[DrsConstants.AES_GCM_IV_LENGTH_BYTES];
            byte[] ct = new byte[full.length - DrsConstants.AES_GCM_IV_LENGTH_BYTES];
            System.arraycopy(full, 0, iv, 0, iv.length);
            System.arraycopy(full, iv.length, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(DrsConstants.AES_GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, "UTF-8");
        } catch (Exception e) {
            throw new DrsException("AES decryption failed", e);
        }
    }

    private static synchronized SecretKey ensureKey() {
        if (secretKey == null) {
            secretKey = generateKey();
        }
        return secretKey;
    }
}
