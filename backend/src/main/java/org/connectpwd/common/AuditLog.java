package org.connectpwd.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLog {

    public void logSessionStart(String userId, String sessionId) {
        log.info("event=session_started userId={} sessionId={}", userId, sessionId);
    }

    public void logSessionComplete(String sessionId) {
        log.info("event=session_completed sessionId={}", sessionId);
    }

    public void logScoreComputed(String sessionId, int totalScore) {
        log.info("event=score_computed sessionId={} totalScore={}", sessionId, totalScore);
    }

    public void logReportGenerated(String sessionId, String reportId) {
        log.info("event=report_generated sessionId={} reportId={}", sessionId, reportId);
    }

    public void logAuthEvent(String event, String email) {
        log.info("event={} email={}", event, email);
    }
}
