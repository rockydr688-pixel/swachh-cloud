package com.cloudclean;

import com.cloudclean.auth.AuthService;
import com.cloudclean.config.JwtTokenProvider;
import com.cloudclean.dto.AuthRequest;
import com.cloudclean.dto.AuthResponse;
import com.cloudclean.dto.RegisterRequest;
import com.cloudclean.exception.DuplicateResourceException;
import com.cloudclean.exception.UnauthorizedException;
import com.cloudclean.repository.UserRepository;
import com.cloudclean.user.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId("user-123");
        sampleUser.setEmail("test@cloudclean.ai");
        sampleUser.setPasswordHash("hashedPassword");
        sampleUser.setFullName("Test User");
        sampleUser.setRole("ROLE_USER");
        sampleUser.setEnabled(true);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Test User", "test@cloudclean.ai", "password123", "Acme Corp");

        when(userRepository.existsByEmail("test@cloudclean.ai")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken(any(), any(), any())).thenReturn("mock-jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("test@cloudclean.ai", response.getUser().getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest request = new RegisterRequest("Test User", "test@cloudclean.ai", "password123", "Acme Corp");
        when(userRepository.existsByEmail("test@cloudclean.ai")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        AuthRequest request = new AuthRequest("test@cloudclean.ai", "password123");

        when(userRepository.findByEmail("test@cloudclean.ai")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(any(), any(), any())).thenReturn("mock-jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("test@cloudclean.ai", response.getUser().getEmail());
    }

    @Test
    void testLoginInvalidPasswordThrowsException() {
        AuthRequest request = new AuthRequest("test@cloudclean.ai", "wrongpassword");

        when(userRepository.findByEmail("test@cloudclean.ai")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}
