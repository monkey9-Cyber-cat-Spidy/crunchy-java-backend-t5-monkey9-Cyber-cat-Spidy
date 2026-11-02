package com.crunchycode.bookstore.service;

import com.crunchycode.bookstore.dto.AuthResponse;
import com.crunchycode.bookstore.dto.LoginRequest;
import com.crunchycode.bookstore.dto.RegisterRequest;
import com.crunchycode.bookstore.exception.DuplicateResourceException;
import com.crunchycode.bookstore.exception.InvalidCredentialsException;
import com.crunchycode.bookstore.model.Role;
import com.crunchycode.bookstore.model.User;
import com.crunchycode.bookstore.repository.UserRepository;
import com.crunchycode.bookstore.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.jwtService = Objects.requireNonNull(jwtService);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered: " + request.email());
        }

        Role assignedRole = Role.ROLE_USER;
        if (request.role() != null && !request.role().isBlank()) {
            try {
                assignedRole = Role.valueOf(request.role().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                assignedRole = Role.ROLE_USER;
            }
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(assignedRole);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), jwtService.getExpirationMs());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), jwtService.getExpirationMs());
    }
}
