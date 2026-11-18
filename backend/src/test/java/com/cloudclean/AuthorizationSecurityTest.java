package com.cloudclean;

import com.cloudclean.config.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationSecurityTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "cloudclean_test_secure_jwt_secret_key_with_at_least_256_bits_length_for_hmac_sha256!";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    void testTokenGenerationAndClaimExtraction() {
        String token = jwtTokenProvider.generateToken("user-test-123", "analyst@cloudclean.ai", "ROLE_USER");
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("user-test-123", jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("analyst@cloudclean.ai", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void testValidateToken_RejectsMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void testValidateToken_RejectsForgedSignature() {
        // Generate with one secret
        JwtTokenProvider anotherProvider = new JwtTokenProvider(
                "another_secret_key_that_is_at_least_256_bits_long_and_completely_different!!",
                3600000
        );
        String forgedToken = anotherProvider.generateToken("attacker", "hacker@evil.com", "ROLE_ADMIN");

        // Validate with original provider
        assertFalse(jwtTokenProvider.validateToken(forgedToken));
    }

    @Test
    void testValidateToken_RejectsExpiredToken() {
        // Provider with negative/zero expiration
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secret, -1000);
        String expiredToken = expiredProvider.generateToken("user-expired", "old@cloudclean.ai", "ROLE_USER");

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
}
