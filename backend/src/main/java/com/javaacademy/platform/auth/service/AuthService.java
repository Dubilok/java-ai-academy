package com.javaacademy.platform.auth.service;

import com.javaacademy.platform.auth.JwtProperties;
import com.javaacademy.platform.auth.dto.AuthResponse;
import com.javaacademy.platform.auth.dto.LoginRequest;
import com.javaacademy.platform.auth.dto.RefreshRequest;
import com.javaacademy.platform.auth.dto.RegisterRequest;
import com.javaacademy.platform.auth.entity.RefreshToken;
import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.RefreshTokenRepository;
import com.javaacademy.platform.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(0L);
        user.setCrystals(0L);
        user.setCreatedAt(Instant.now(clock));
        User saved = userRepository.save(user);
        log.info("Registered new user: {}", saved.getEmail());
        return new AuthResponse(jwtService.generateAccessToken(saved), issueRefreshToken(saved));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResponse(jwtService.generateAccessToken(user), issueRefreshToken(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked()) {
            revokeAllForUser(stored.getUser());
            log.warn(
                    "Refresh token reuse detected for user {}", stored.getUser().getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected");
        }

        if (stored.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return new AuthResponse(jwtService.generateAccessToken(user), issueRefreshToken(user));
    }

    private String issueRefreshToken(User user) {
        String raw = UUID.randomUUID().toString();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(Instant.now(clock).plus(jwtProperties.refreshTokenExpiryDays(), ChronoUnit.DAYS));
        token.setRevoked(false);
        token.setCreatedAt(Instant.now(clock));
        refreshTokenRepository.save(token);
        return raw;
    }

    private void revokeAllForUser(User user) {
        List<RefreshToken> all = refreshTokenRepository.findAllByUser(user);
        all.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(all);
    }

    public static String sha256(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
