package org.connectpwd.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.connectpwd.auth.dto.AuthResponse;
import org.connectpwd.auth.dto.LoginRequest;
import org.connectpwd.auth.dto.RefreshRequest;
import org.connectpwd.auth.dto.RegisterRequest;
import org.connectpwd.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenRefreshService tokenRefreshService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = tokenRefreshService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
