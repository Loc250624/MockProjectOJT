package com.ojtsu26.elearning.seeder;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs once at application startup.
 * Finds every LOCAL user whose password_hash is stored as plain text
 * (i.e. does NOT start with "$2a$") and re-encodes it with BCrypt.
 *
 * Safe to run on every startup: users that already have a BCrypt hash
 * are skipped automatically.
 */
@Slf4j
@Component
@Order(1)          // Run before any other CommandLineRunner
@RequiredArgsConstructor
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    // Use a dedicated BCryptPasswordEncoder directly (strength 10 = Spring default)
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(String... args) {
        List<User> users = userRepository.findAll();

        int migrated = 0;
        for (User user : users) {
            String hash = user.getPasswordHash();
            // Skip OAuth2 users (no local password) and already-hashed passwords
            if (hash == null || hash.startsWith("$2a$") || hash.startsWith("$2b$")) {
                continue;
            }
            // Plain-text password found — encode it
            String newHash = bcrypt.encode(hash);
            user.setPasswordHash(newHash);
            userRepository.save(user);
            migrated++;
            log.info("[PasswordMigration] Migrated user id={} email={}", user.getId(), user.getEmail());
        }

        if (migrated > 0) {
            log.info("[PasswordMigration] Migration complete. {} user(s) updated.", migrated);
        } else {
            log.info("[PasswordMigration] All passwords are already BCrypt-encoded. Nothing to migrate.");
        }
    }
}
