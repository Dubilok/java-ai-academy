package com.javaacademy.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.javaacademy.platform.auth.dto.AuthResponse;
import com.javaacademy.platform.auth.dto.LoginRequest;
import com.javaacademy.platform.auth.dto.RefreshRequest;
import com.javaacademy.platform.auth.dto.RegisterRequest;
import com.javaacademy.platform.auth.entity.RefreshToken;
import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.RefreshTokenRepository;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.auth.service.AuthService;
import com.javaacademy.platform.auth.service.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    Clock fixedClock = Clock.fixed(Instant.parse("2026-07-24T10:00:00Z"), ZoneOffset.UTC);
    JwtProperties jwtProperties = new JwtProperties("test-secret-32-chars-minimum!!!!", 900_000L, 30L);

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, refreshTokenRepository, jwtProperties, fixedClock);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_withNewEmail_savesUserWithCorrectFields() {
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("$2a$12$hashed");
        User savedUser = userWithEmail("new@example.com");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateAccessToken(savedUser)).willReturn("access-token");

        authService.register(new RegisterRequest("new@example.com", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getEmail()).isEqualTo("new@example.com");
        assertThat(persisted.getPasswordHash()).isEqualTo("$2a$12$hashed");
        assertThat(persisted.getRole()).isEqualTo("ROLE_STUDENT");
        assertThat(persisted.getXpPoints()).isZero();
        assertThat(persisted.getCrystals()).isZero();
        assertThat(persisted.getCreatedAt()).isEqualTo(Instant.parse("2026-07-24T10:00:00Z"));
    }

    @Test
    void register_withNewEmail_returnsAccessTokenFromJwtService() {
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("hashed");
        User savedUser = userWithEmail("new@example.com");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateAccessToken(savedUser)).willReturn("my-access-token");

        AuthResponse response = authService.register(new RegisterRequest("new@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("my-access-token");
    }

    @Test
    void register_withNewEmail_persistsHashedRefreshTokenNotRaw() {
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("hashed");
        User savedUser = userWithEmail("new@example.com");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateAccessToken(any())).willReturn("access");

        AuthResponse response = authService.register(new RegisterRequest("new@example.com", "password123"));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken persisted = captor.getValue();
        // hash must differ from the raw token returned to the client
        assertThat(persisted.getTokenHash()).isNotEqualTo(response.refreshToken());
        assertThat(persisted.getTokenHash()).isEqualTo(AuthService.sha256(response.refreshToken()));
        assertThat(persisted.isRevoked()).isFalse();
        assertThat(persisted.getExpiresAt())
                .isEqualTo(Instant.parse("2026-07-24T10:00:00Z").plusSeconds(30L * 86_400));
    }

    @Test
    void register_withDuplicateEmail_throwsConflictAndNeverSaves() {
        given(userRepository.findByEmail("dup@example.com")).willReturn(Optional.of(userWithEmail("dup@example.com")));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("dup@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void register_encodesPasswordBeforePersisting() {
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode("plaintext")).willReturn("bcrypt-hash");
        given(userRepository.save(any(User.class))).willReturn(userWithEmail("x@x.com"));
        given(jwtService.generateAccessToken(any())).willReturn("token");

        authService.register(new RegisterRequest("x@x.com", "plaintext"));

        verify(passwordEncoder).encode("plaintext");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_withCorrectCredentials_returnsTokenPairAndPersistsRefreshToken() {
        User user = userWithEmail("user@example.com");
        user.setPasswordHash("$2a$12$hashed");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "$2a$12$hashed")).willReturn(true);
        given(jwtService.generateAccessToken(user)).willReturn("valid-access");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("valid-access");
        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        given(userRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "anything")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_withWrongPassword_throwsUnauthorizedAndNeverIssuesToken() {
        User user = userWithEmail("user@example.com");
        user.setPasswordHash("$2a$12$hashed");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "$2a$12$hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_withWrongPassword_andUnknownEmail_returnSameStatusToPreventUserEnumeration() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());
        given(userRepository.findByEmail("real@example.com"))
                .willReturn(Optional.of(userWithEmail("real@example.com")));
        given(passwordEncoder.matches(any(), any())).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "pass")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> authService.login(new LoginRequest("real@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_withValidToken_revokesOldAndReturnsNewPair() {
        String rawToken = "raw-refresh-token";
        String hash = AuthService.sha256(rawToken);
        User user = userWithEmail("user@example.com");
        RefreshToken stored = activeToken(user, hash, Instant.parse("2026-08-24T10:00:00Z"));
        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));
        given(jwtService.generateAccessToken(user)).willReturn("new-access");

        AuthResponse response = authService.refresh(new RefreshRequest(rawToken));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void refresh_withUnknownToken_throwsUnauthorized() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_withRevokedToken_revokesAllUserTokensAndThrowsUnauthorized() {
        String rawToken = "revoked-token";
        String hash = AuthService.sha256(rawToken);
        User user = userWithEmail("user@example.com");
        RefreshToken stored = activeToken(user, hash, Instant.parse("2026-08-24T10:00:00Z"));
        stored.setRevoked(true);
        RefreshToken anotherToken = activeToken(user, "other-hash", Instant.parse("2026-08-24T10:00:00Z"));
        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));
        given(refreshTokenRepository.findAllByUser(user)).willReturn(List.of(stored, anotherToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(rawToken)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        // Both tokens for this user must be revoked
        assertThat(stored.isRevoked()).isTrue();
        assertThat(anotherToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(stored, anotherToken));
    }

    @Test
    void refresh_withExpiredToken_throwsUnauthorized() {
        String rawToken = "expired-token";
        String hash = AuthService.sha256(rawToken);
        User user = userWithEmail("user@example.com");
        // Expiry is in the past relative to fixedClock
        RefreshToken stored = activeToken(user, hash, Instant.parse("2026-01-01T00:00:00Z"));
        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(rawToken)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(jwtService, never()).generateAccessToken(any());
    }

    // ── sha256 helper ─────────────────────────────────────────────────────────

    @Test
    void sha256_sameInputProducesSameHash() {
        assertThat(AuthService.sha256("hello")).isEqualTo(AuthService.sha256("hello"));
    }

    @Test
    void sha256_differentInputsProduceDifferentHashes() {
        assertThat(AuthService.sha256("a")).isNotEqualTo(AuthService.sha256("b"));
    }

    @Test
    void sha256_outputIs64HexChars() {
        assertThat(AuthService.sha256("any input")).hasSize(64);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User userWithEmail(String email) {
        User user = new User();
        user.setEmail(email);
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(0L);
        user.setCrystals(0L);
        user.setCreatedAt(Instant.now(fixedClock));
        return user;
    }

    private RefreshToken activeToken(User user, String hash, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(expiresAt);
        token.setRevoked(false);
        token.setCreatedAt(Instant.now(fixedClock));
        return token;
    }
}
