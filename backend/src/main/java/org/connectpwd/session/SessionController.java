package org.connectpwd.session;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.connectpwd.common.ApiResponse;
import org.connectpwd.session.dto.SessionResponse;
import org.connectpwd.session.dto.StartSessionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SessionResponse>> startSession(
            @Valid @RequestBody StartSessionRequest request,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        SessionResponse response = sessionService.startSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @PathVariable UUID sessionId,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("CAREGIVER");

        SessionResponse response = sessionService.getSession(sessionId, userId, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{sessionId}/advance")
    public ResponseEntity<ApiResponse<SessionResponse>> advanceLevel(
            @PathVariable UUID sessionId,
            @RequestBody java.util.Map<String, Integer> body,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("CAREGIVER");

        int nextLevel = body.getOrDefault("level", 1);
        SessionResponse response = sessionService.advanceLevel(sessionId, nextLevel, userId, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
