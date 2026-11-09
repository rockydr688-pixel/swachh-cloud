package com.cloudclean;

import com.cloudclean.util.EncryptionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionUtilsTest {

    @Test
    void testEncryptionDecryptionRoundtrip() {
        EncryptionUtils encryptionUtils = new EncryptionUtils("my_super_secure_32_byte_secret_key_123!");

        String originalOAuthToken = "ya29.a0AfH6SMA...google-oauth-access-token-sample";
        String encrypted = encryptionUtils.encrypt(originalOAuthToken);

        assertNotNull(encrypted);
        assertNotEquals(originalOAuthToken, encrypted);

        String decrypted = encryptionUtils.decrypt(encrypted);
        assertEquals(originalOAuthToken, decrypted);
    }

    @Test
    void testNullOrBlankHandling() {
        EncryptionUtils encryptionUtils = new EncryptionUtils("test-secret");
        assertNull(encryptionUtils.encrypt(null));
        assertNull(encryptionUtils.decrypt(null));
        assertEquals("", encryptionUtils.encrypt(""));
        assertEquals("", encryptionUtils.decrypt(""));
    }
}
