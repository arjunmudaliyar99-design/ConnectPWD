package org.connectpwd.auth;

import org.connectpwd.auth.dto.AuthResponse;
import org.connectpwd.auth.dto.LoginRequest;
import org.connectpwd.auth.dto.RegisterRequest;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.user.User;
import org.connectpwd.user.UserRepository;
import org.connectpwd.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditLog auditLog;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole("CAREGIVER");
        registerRequest.setLanguage("en");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        savedUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("$2a$12$encoded")
                .role(UserRole.CAREGIVER)
                .language("en")
                .build();
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any(UUID.class), eq("CAREGIVER"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UUID.class), eq("CAREGIVER"))).thenReturn("refresh-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo("CAREGIVER");
        verify(userRepository).save(any(User.class));
        verify(auditLog).logAuthEvent("register", "test@example.com");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "$2a$12$encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UUID.class), eq("CAREGIVER"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UUID.class), eq("CAREGIVER"))).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRole()).isEqualTo("CAREGIVER");
        verify(auditLog).logAuthEvent("login", "test@example.com");
    }

    @Test
    void login_invalidEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_invalidPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "$2a$12$encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid email or password");
    }
}
