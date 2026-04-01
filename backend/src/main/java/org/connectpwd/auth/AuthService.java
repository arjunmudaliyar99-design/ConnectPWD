package org.connectpwd.auth;

import lombok.RequiredArgsConstructor;
import org.connectpwd.auth.dto.AuthResponse;
import org.connectpwd.auth.dto.LoginRequest;
import org.connectpwd.auth.dto.RegisterRequest;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.user.User;
import org.connectpwd.user.UserRepository;
import org.connectpwd.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLog auditLog;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict(ErrorCode.DUPLICATE_EMAIL, "Email already registered");
        }

        UserRole role = request.getRole() != null
                ? UserRole.valueOf(request.getRole())
                : UserRole.CAREGIVER;

        String language = request.getLanguage() != null ? request.getLanguage() : "en";

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .language(language)
                .phone(request.getPhone())
                .build();

        user = userRepository.save(user);
        auditLog.logAuthEvent("register", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AppException.unauthorized(ErrorCode.AUTHENTICATION_FAILED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.unauthorized(ErrorCode.AUTHENTICATION_FAILED, "Invalid email or password");
        }

        auditLog.logAuthEvent("login", user.getEmail());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId().toString())
                .role(user.getRole().name())
                .language(user.getLanguage())
                .build();
    }
}
