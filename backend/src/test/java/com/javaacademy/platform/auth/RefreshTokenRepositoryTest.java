package com.javaacademy.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
class RefreshTokenRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    UserRepository userRepository;

    // ── findByTokenHash ───────────────────────────────────────────────────────

    @Test
    void findByTokenHash_whenTokenExists_returnsToken() {
        User user = savedUser("find@example.com");
        RefreshToken token = savedToken(user, "hash-abc", Instant.parse("2027-01-01T00:00:00Z"), false);

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-abc");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(token.getId());
        assertThat(found.get().isRevoked()).isFalse();
    }

    @Test
    void findByTokenHash_whenHashUnknown_returnsEmpty() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("nonexistent-hash");
        assertThat(found).isEmpty();
    }

    @Test
    void findByTokenHash_uniqueConstraintPreventsHashCollision() {
        User user = savedUser("collision@example.com");
        savedToken(user, "duplicate-hash", Instant.parse("2027-01-01T00:00:00Z"), false);

        RefreshToken dup = tokenFor(user, "duplicate-hash", Instant.parse("2027-06-01T00:00:00Z"), false);
        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── findAllByUser ─────────────────────────────────────────────────────────

    @Test
    void findAllByUser_returnsOnlyThatUsersTokens() {
        User user1 = savedUser("user1@example.com");
        User user2 = savedUser("user2@example.com");
        savedToken(user1, "hash-u1-a", Instant.parse("2027-01-01T00:00:00Z"), false);
        savedToken(user1, "hash-u1-b", Instant.parse("2027-01-01T00:00:00Z"), true);
        savedToken(user2, "hash-u2-a", Instant.parse("2027-01-01T00:00:00Z"), false);

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user1);

        assertThat(tokens).hasSize(2);
        assertThat(tokens).allMatch(t -> t.getUser().getId().equals(user1.getId()));
    }

    @Test
    void findAllByUser_whenNoTokens_returnsEmptyList() {
        User user = savedUser("empty@example.com");
        assertThat(refreshTokenRepository.findAllByUser(user)).isEmpty();
    }

    // ── cascade delete ────────────────────────────────────────────────────────

    @Test
    void deleteUser_cascadesToRefreshTokens() {
        User user = savedUser("cascade@example.com");
        savedToken(user, "hash-cascade", Instant.parse("2027-01-01T00:00:00Z"), false);

        userRepository.delete(user);
        userRepository.flush();

        assertThat(refreshTokenRepository.findByTokenHash("hash-cascade")).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User savedUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$12$hash");
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(0L);
        user.setCrystals(0L);
        user.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        return userRepository.saveAndFlush(user);
    }

    private RefreshToken savedToken(User user, String hash, Instant expiresAt, boolean revoked) {
        RefreshToken token = tokenFor(user, hash, expiresAt, revoked);
        return refreshTokenRepository.saveAndFlush(token);
    }

    private RefreshToken tokenFor(User user, String hash, Instant expiresAt, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(expiresAt);
        token.setRevoked(revoked);
        token.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        return token;
    }
}
