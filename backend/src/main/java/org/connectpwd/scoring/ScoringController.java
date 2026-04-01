package org.connectpwd.scoring;

import lombok.RequiredArgsConstructor;
import org.connectpwd.common.ApiResponse;
import org.connectpwd.scoring.dto.IsaaScoreDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session/{sessionId}/score")
@RequiredArgsConstructor
public class ScoringController {

    private final IsaaScoreService isaaScoreService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CAREGIVER', 'PSYCHOLOGIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<IsaaScoreDTO>> computeScore(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("CAREGIVER");
        IsaaScoreDTO score = isaaScoreService.computeScore(sessionId, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(score));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CAREGIVER', 'PSYCHOLOGIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<IsaaScoreDTO>> getScore(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("CAREGIVER");
        IsaaScoreDTO score = isaaScoreService.getScore(sessionId, userId, role);
        return ResponseEntity.ok(ApiResponse.ok(score));
    }
}
