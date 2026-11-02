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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "testuser", "test@example.com", "encoded_password", Role.ROLE_USER);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest("newuser", "new@example.com", "password123", "ROLE_USER");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(jwtService.generateToken("newuser", "ROLE_USER")).thenReturn("jwt_token_123");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("newuser", res.username());
        assertEquals("ROLE_USER", res.role());
        assertEquals("jwt_token_123", res.token());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateUsernameThrowsException() {
        RegisterRequest req = new RegisterRequest("existinguser", "new@example.com", "password123", null);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest req = new RegisterRequest("newuser", "existing@example.com", "password123", null);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest("testuser", "correct_pass");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correct_pass", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken("testuser", "ROLE_USER")).thenReturn("token_abc");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("token_abc", res.token());
        assertEquals("testuser", res.username());
    }

    @Test
    void testLoginInvalidUserThrowsException() {
        LoginRequest req = new LoginRequest("unknown", "pass");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void testLoginWrongPasswordThrowsException() {
        LoginRequest req = new LoginRequest("testuser", "wrong_pass");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }
}
