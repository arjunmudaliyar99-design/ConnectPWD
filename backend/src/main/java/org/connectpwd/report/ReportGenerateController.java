package org.connectpwd.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.connectpwd.report.dto.ReportGenerateRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * POST /api/v1/report/generate
 *
 * Accepts { "sessionId": "..." } and streams the generated PDF directly
 * to the browser — no R2/S3 storage required.
 */
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportGenerateController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'PSYCHOLOGIST', 'ADMIN')")
    public ResponseEntity<StreamingResponseBody> generateReport(
            @Valid @RequestBody ReportGenerateRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");

        byte[] pdfBytes = reportService.generateReportBytes(request.getSessionId(), userId, role);

        StreamingResponseBody body = out -> {
            out.write(pdfBytes);
            out.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"report-" + request.getSessionId() + ".pdf\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdfBytes.length))
                .body(body);
    }
}
