package org.connectpwd.report;

import lombok.RequiredArgsConstructor;
import org.connectpwd.common.ApiResponse;
import org.connectpwd.report.dto.ReportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session/{sessionId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CAREGIVER', 'PSYCHOLOGIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable String sessionId,
            Authentication authentication) {
        String userId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        ReportResponse response = reportService.getReport(sessionId, userId, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
