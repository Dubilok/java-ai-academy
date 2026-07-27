package com.javaacademy.platform.config;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a default admin account on startup when running with the {@code local} profile.
 * Credentials: admin@academy.local / admin123
 *
 * <p>Only active with {@code --spring.profiles.active=local}. Never runs in production.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@academy.local";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.debug("Dev admin account already exists, skipping creation.");
            return;
        }

        User admin = new User();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(ROLE_ADMIN);
        admin.setXpPoints(0L);
        admin.setCrystals(0L);
        admin.setCreatedAt(Instant.now(clock));
        userRepository.save(admin);

        log.info("Dev admin account created — email: {} / password: {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
