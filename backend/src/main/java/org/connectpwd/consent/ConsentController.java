package org.connectpwd.consent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.connectpwd.common.ApiResponse;
import org.connectpwd.consent.dto.ConsentRequest;
import org.connectpwd.consent.dto.ConsentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CAREGIVER', 'PSYCHOLOGIST')")
    public ResponseEntity<ApiResponse<ConsentResponse>> createConsent(
            @Valid @RequestBody ConsentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) authentication.getPrincipal();
        String ipAddress = httpRequest.getRemoteAddr();

        ConsentResponse response = consentService.createConsent(userId, request, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
