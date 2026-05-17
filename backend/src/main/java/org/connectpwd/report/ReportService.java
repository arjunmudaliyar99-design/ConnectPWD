package org.connectpwd.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.answer.ResponseRepository;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.report.dto.ReportResponse;
import org.connectpwd.user.User;
import org.connectpwd.user.UserService;
import org.connectpwd.scoring.IsaaScore;
import org.connectpwd.scoring.IsaaScoreRepository;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.connectpwd.storage.StorageService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReportService {

        private final ReportRepository reportRepository;
        private final IsaaScoreRepository isaaScoreRepository;
        private final ResponseRepository responseRepository;
        private final SessionService sessionService;
        private final UserService userService;
        private final StorageService storageService;
        private final PdfGenerator pdfGenerator;
        private final AuditLog auditLog;

        private static final int ISAA_TOTAL_ITEMS = 40;

        /**
         * Generates the ISAA PDF and returns the raw bytes directly — no R2 upload.
         * Fetches all response documents for the session, filters out any with null
         * answerText/domain for null safety, then validates completeness.
         */
        public byte[] generateReportBytes(String sessionId, String userId, String userRole) {
                log.info("Generating report for sessionId: {}", sessionId);

                AssessmentSession session = sessionService.findById(sessionId);
                sessionService.checkAccess(session, userId, userRole);

                // Module sessions (PARENT, ADULT_SELF, etc.) don't produce an ISAA score.
                // The ISAA PDF is only meaningful for full level-2 assessments.
                if (session.getModuleType() != null) {
                        throw AppException.badRequest(ErrorCode.VALIDATION_ERROR,
                                        "Detailed ISAA report is not available for module assessments ("
                                                        + session.getModuleType()
                                                        + "). Only full ISAA assessments produce a PDF report.");
                }

                // Count all responses for this session regardless of level, filtering out
                // documents with missing answerText or domain (null safety).
                long validAnswerCount = responseRepository.findBySessionId(sessionId)
                                .stream()
                                .filter(r -> r.getDomain() != null
                                                && (r.getAnswerText() != null || r.getScaleValue() != null))
                                .count();
                log.info("Valid answer count for sessionId {}: {}", sessionId, validAnswerCount);

                IsaaScore score = isaaScoreRepository.findBySessionId(sessionId)
                                .orElseGet(() -> {
                                        if (validAnswerCount < ISAA_TOTAL_ITEMS) {
                                                throw AppException.badRequest(ErrorCode.VALIDATION_ERROR,
                                                                "Assessment incomplete: " + validAnswerCount + " of "
                                                                                + ISAA_TOTAL_ITEMS
                                                                                + " questions answered. Complete all questions before generating a report.");
                                        }
                                        throw AppException.notFound(ErrorCode.SCORE_NOT_FOUND,
                                                        "Score not yet computed — call POST /api/v1/session/"
                                                                        + sessionId + "/score first.");
                                });

                User sessionUser = userService.findById(session.getUserId());
                String clientName = sessionUser != null ? sessionUser.getFullName() : "Unknown";

                log.info("Generating PDF for sessionId: {} client: {}", sessionId, clientName);
                return pdfGenerator.generateReport(score, clientName, clientName, session.getLanguage());
        }

        public ReportResponse getReport(String sessionId, String userId, String userRole) {
                AssessmentSession session = sessionService.findById(sessionId);
                sessionService.checkAccess(session, userId, userRole);

                Report report = reportRepository.findBySessionId(sessionId)
                                .orElseThrow(() -> AppException.notFound(ErrorCode.REPORT_NOT_FOUND,
                                                "Report not found"));

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
