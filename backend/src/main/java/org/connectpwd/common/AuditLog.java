package org.connectpwd.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class AuditLog {

    public void logConsentSigned(UUID userId, String ipAddress) {
        log.info("event=consent_signed userId={} ip={}", userId, ipAddress);
    }

    public void logSessionStart(UUID userId, UUID sessionId) {
        log.info("event=session_started userId={} sessionId={}", userId, sessionId);
    }

    public void logSessionComplete(UUID sessionId) {
        log.info("event=session_completed sessionId={}", sessionId);
    }

    public void logScoreComputed(UUID sessionId, int totalScore) {
        log.info("event=score_computed sessionId={} totalScore={}", sessionId, totalScore);
    }

    public void logReportGenerated(UUID sessionId, UUID reportId) {
        log.info("event=report_generated sessionId={} reportId={}", sessionId, reportId);
    }

    public void logAuthEvent(String event, String email) {
        log.info("event={} email={}", event, email);
    }
}
