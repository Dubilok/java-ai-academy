package com.javaacademy.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

    Clock fixedClock = Clock.fixed(Instant.parse("2026-07-24T10:00:00Z"), ZoneOffset.UTC);

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, fixedClock);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_withNewEmail_savesUserWithCorrectFields() {
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("$2a$12$hashed");
        User savedUser = userWithEmail("new@example.com");
        given(userRepository.save(any())).willReturn(savedUser);
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
        given(userRepository.save(any())).willReturn(savedUser);
        given(jwtService.generateAccessToken(savedUser)).willReturn("my-access-token");

        AuthResponse response = authService.register(new RegisterRequest("new@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("my-access-token");
    }

    @Test
    void register_withNewEmail_returnsNonNullRefreshToken() {
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("hashed");
        given(userRepository.save(any())).willReturn(userWithEmail("new@example.com"));
        given(jwtService.generateAccessToken(any())).willReturn("access");

        AuthResponse response = authService.register(new RegisterRequest("new@example.com", "password123"));

        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void register_withDuplicateEmail_throwsConflict() {
        given(userRepository.findByEmail("dup@example.com")).willReturn(Optional.of(userWithEmail("dup@example.com")));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("dup@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_encodesPasswordBeforePersisting() {
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode("plaintext")).willReturn("bcrypt-hash");
        given(userRepository.save(any())).willReturn(userWithEmail("x@x.com"));
        given(jwtService.generateAccessToken(any())).willReturn("token");

        authService.register(new RegisterRequest("x@x.com", "plaintext"));

        verify(passwordEncoder).encode("plaintext");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_withCorrectCredentials_returnsAccessToken() {
        User user = userWithEmail("user@example.com");
        user.setPasswordHash("$2a$12$hashed");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "$2a$12$hashed")).willReturn(true);
        given(jwtService.generateAccessToken(user)).willReturn("valid-access");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("valid-access");
        assertThat(response.refreshToken()).isNotBlank();
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
    void login_withWrongPassword_throwsUnauthorized() {
        User user = userWithEmail("user@example.com");
        user.setPasswordHash("$2a$12$hashed");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "$2a$12$hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(jwtService, never()).generateAccessToken(any());
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
}
