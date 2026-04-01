package org.connectpwd.auth;

import lombok.RequiredArgsConstructor;
import org.connectpwd.auth.dto.RefreshRequest;
import org.connectpwd.auth.dto.AuthResponse;
import org.connectpwd.common.AppException;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.user.User;
import org.connectpwd.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw AppException.unauthorized(ErrorCode.TOKEN_EXPIRED, "Refresh token expired or invalid");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.unauthorized(ErrorCode.USER_NOT_FOUND, "User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId().toString())
                .role(user.getRole().name())
                .language(user.getLanguage())
                .build();
    }
}
