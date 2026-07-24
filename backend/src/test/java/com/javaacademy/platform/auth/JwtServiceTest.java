package com.javaacademy.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.service.JwtService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    static final String SECRET = "test-secret-at-least-32-characters-long!!";
    static final long EXPIRY_MS = 900_000L; // 15 min

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, EXPIRY_MS, 30L));
    }

    @Test
    void generateAccessToken_producesNonBlankToken() {
        assertThat(jwtService.generateAccessToken(userWithEmail("a@a.com"))).isNotBlank();
    }

    @Test
    void extractEmail_returnsSubjectEmbeddedInToken() {
        String token = jwtService.generateAccessToken(userWithEmail("user@example.com"));
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForFreshToken() {
        String token = jwtService.generateAccessToken(userWithEmail("user@example.com"));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedSignature() {
        String token = jwtService.generateAccessToken(userWithEmail("user@example.com"));
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithDifferentKey() {
        JwtService otherService =
                new JwtService(new JwtProperties("completely-different-secret-key-!!", EXPIRY_MS, 30L));
        String foreignToken = otherService.generateAccessToken(userWithEmail("user@example.com"));
        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        JwtService expiredService = new JwtService(new JwtProperties(SECRET, 1L, 30L));
        String token = expiredService.generateAccessToken(userWithEmail("user@example.com"));
        // 1 ms expiry — guaranteed expired by the time we validate
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForGarbage() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void generateAccessToken_differentUsersProduceDifferentTokens() {
        String t1 = jwtService.generateAccessToken(userWithEmail("alice@example.com"));
        String t2 = jwtService.generateAccessToken(userWithEmail("bob@example.com"));
        assertThat(t1).isNotEqualTo(t2);
    }

    private User userWithEmail(String email) {
        User user = new User();
        user.setEmail(email);
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(0L);
        user.setCrystals(0L);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
