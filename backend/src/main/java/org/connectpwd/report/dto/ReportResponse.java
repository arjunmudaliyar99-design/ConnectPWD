package org.connectpwd.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String id;
    private String sessionId;
    private String pdfUrl;
    private String language;
    private Instant generatedAt;
}
