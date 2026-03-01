package com.waitlist.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();
        // tweak the secret and expiration via reflection so that the token
        // generation/validation behaviour is predictable
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        // 32 bytes to satisfy HMAC SHA-256 requirement
        secretField.set(provider, "01234567890123456789012345678901");

        Field mins = JwtTokenProvider.class.getDeclaredField("jwtExpirationMinutes");
        mins.setAccessible(true);
        mins.set(provider, 5);
    }

    @Test
    public void generateToken_shouldContainUsernameAndUserId() {
        String token = provider.generateToken("alice", 42L);
        assertNotNull(token);

        assertTrue(provider.validateToken(token));
        assertEquals("alice", provider.getUsernameFromToken(token));
        assertEquals(42L, provider.getUserIdFromToken(token));
    }

    @Test
    public void validateToken_shouldReturnFalseForGarbage() {
        assertFalse(provider.validateToken("not-a-token"));
    }
}
