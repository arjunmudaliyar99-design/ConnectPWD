package org.connectpwd.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.consent.Consent;
import org.connectpwd.consent.ConsentService;
import org.connectpwd.report.dto.ReportResponse;
import org.connectpwd.scoring.IsaaScore;
import org.connectpwd.scoring.IsaaScoreRepository;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.connectpwd.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final IsaaScoreRepository isaaScoreRepository;
    private final SessionService sessionService;
    private final ConsentService consentService;
    private final StorageService storageService;
    private final PdfGenerator pdfGenerator;
    private final AuditLog auditLog;

    @Transactional
    public ReportResponse generateReport(UUID sessionId, UUID userId, String userRole) {
        AssessmentSession session = sessionService.findById(sessionId);
        sessionService.checkAccess(session, userId, userRole);

        if (reportRepository.findBySessionId(sessionId).isPresent()) {
            return getReport(sessionId, userId, userRole);
        }

        IsaaScore score = isaaScoreRepository.findBySessionId(sessionId)
                .orElseThrow(() -> AppException.notFound(ErrorCode.SCORE_NOT_FOUND, "ISAA score not found — score the session first"));

        Consent consent = consentService.findById(session.getConsentId());
        String clientName = consent != null ? consent.getClientName() : "Unknown";
        String caregiverName = consent != null ? consent.getLegalName() : "Unknown";

        byte[] pdfBytes = pdfGenerator.generateReport(score, clientName, caregiverName, session.getLanguage());
        String pdfKey = storageService.uploadPdf(sessionId, pdfBytes);

        Report report = Report.builder()
                .sessionId(sessionId)
                .isaaScoreId(score.getId())
                .pdfUrl(pdfKey)
                .language(session.getLanguage())
                .build();

        report = reportRepository.save(report);
        auditLog.logReportGenerated(sessionId, report.getId());

        String presignedUrl = storageService.generatePresignedUrl(pdfKey);

        return ReportResponse.builder()
                .id(report.getId())
                .sessionId(sessionId)
                .pdfUrl(presignedUrl)
                .language(report.getLanguage())
                .generatedAt(report.getGeneratedAt())
                .build();
    }

    public ReportResponse getReport(UUID sessionId, UUID userId, String userRole) {
        AssessmentSession session = sessionService.findById(sessionId);
        sessionService.checkAccess(session, userId, userRole);

        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> AppException.notFound(ErrorCode.REPORT_NOT_FOUND, "Report not found"));

        String presignedUrl = storageService.generatePresignedUrl(report.getPdfUrl());

        return ReportResponse.builder()
                .id(report.getId())
                .sessionId(sessionId)
                .pdfUrl(presignedUrl)
                .language(report.getLanguage())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
}
