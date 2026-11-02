package com.crunchycode.bookstore.config;

import com.crunchycode.bookstore.model.Role;
import com.crunchycode.bookstore.model.User;
import com.crunchycode.bookstore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes and synchronizes default credentials for the live demo and RBAC playground.
 * Ensures the admin and user accounts have valid BCrypt password hashes matching the documentation.
 */
@Component
public class DefaultUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureUser("admin", "admin@crunchycode.com", "admin123", Role.ROLE_ADMIN);
        ensureUser("user", "user@crunchycode.com", "user123", Role.ROLE_USER);
        ensureUser("regular_user", "regular_user@crunchycode.com", "user123", Role.ROLE_USER);
        log.info("Default user accounts verified and initialized successfully.");
    }

    private void ensureUser(String username, String email, String rawPassword, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    userRepository.save(user);
                    log.info("Synchronized credentials for user: {}", username);
                },
                () -> {
                    // Avoid email collision if email already exists under different username
                    String finalEmail = userRepository.existsByEmail(email) 
                            ? username + "@crunchycode.internal" 
                            : email;

                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(finalEmail);
                    newUser.setPasswordHash(passwordEncoder.encode(rawPassword));
                    newUser.setRole(role);
                    userRepository.save(newUser);
                    log.info("Created default user: {} with role: {}", username, role);
                }
        );
    }
}
