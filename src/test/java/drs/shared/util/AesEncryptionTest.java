package drs.shared.util;

import drs.shared.exception.DrsException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AesEncryption}.
  
 */
class AesEncryptionTest {

    @BeforeAll
    static void setUp() {
        SecretKey key = AesEncryption.generateKey();
        AesEncryption.init(key);
    }

    @Test
    void testEncryptDecryptRoundTrip() {
        String plaintext = "Major fire at Sydney CBD, evacuating buildings.";
        String ciphertext = AesEncryption.encrypt(plaintext);
        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);
        assertEquals(plaintext, AesEncryption.decrypt(ciphertext));
    }

    @Test
    void testNullPlaintextReturnsNull() {
        assertNull(AesEncryption.encrypt(null));
        assertNull(AesEncryption.decrypt(null));
    }

    @Test
    void testTamperedCiphertextFails() {
        String plaintext = "Sensitive data";
        String ciphertext = AesEncryption.encrypt(plaintext);
        // Flip a character in the middle of the base64
        char[] chars = ciphertext.toCharArray();
        chars[chars.length / 2] = chars[chars.length / 2] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);
        assertThrows(DrsException.class, () -> AesEncryption.decrypt(tampered));
    }
}
