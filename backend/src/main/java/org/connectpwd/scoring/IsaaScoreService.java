package org.connectpwd.scoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.answer.ResponseDocument;
import org.connectpwd.answer.ResponseRepository;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.scoring.dto.IsaaScoreDTO;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * IsaaScoreService — the ONLY class that performs ISAA scoring.
 * Scoring logic derived from the NIMH ISAA Manual published by The National Trust, Government of India.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IsaaScoreService {

    private final ResponseRepository responseRepository;
    private final IsaaScoreRepository isaaScoreRepository;
    private final SessionService sessionService;
    private final AuditLog auditLog;

    private static final int ISAA_TOTAL_ITEMS = 40;

    // Domain ranges: items are 1-based indices
    // Domain 1 Social Relationship & Reciprocity: items 1–9
    // Domain 2 Emotional Responsiveness: items 10–14
    // Domain 3 Speech, Language & Communication: items 15–23
    // Domain 4 Behaviour Patterns: items 24–30
    // Domain 5 Sensory Aspects: items 31–36
    // Domain 6 Cognitive Component: items 37–40

    @Transactional
    public IsaaScoreDTO computeScore(UUID sessionId, UUID userId, String userRole) {
        AssessmentSession session = sessionService.findById(sessionId);
        sessionService.checkAccess(session, userId, userRole);

        if (isaaScoreRepository.existsBySessionId(sessionId)) {
            return toDTO(isaaScoreRepository.findBySessionId(sessionId).get());
        }

        List<ResponseDocument> responses = responseRepository.findBySessionIdAndLevel(sessionId.toString(), 2);

        if (responses.size() < ISAA_TOTAL_ITEMS) {
            throw AppException.badRequest(ErrorCode.SCORING_NOT_READY,
                    "All 40 ISAA items must be answered. Current: " + responses.size());
        }

        int[] scaleValues = new int[ISAA_TOTAL_ITEMS + 1]; // 1-based index
        for (ResponseDocument r : responses) {
            if (r.getScaleValue() != null && r.getQuestionIndex() >= 0 && r.getQuestionIndex() < ISAA_TOTAL_ITEMS) {
                scaleValues[r.getQuestionIndex() + 1] = r.getScaleValue();
            }
        }

        int domain1 = sumRange(scaleValues, 1, 9);
        int domain2 = sumRange(scaleValues, 10, 14);
        int domain3 = sumRange(scaleValues, 15, 23);
        int domain4 = sumRange(scaleValues, 24, 30);
        int domain5 = sumRange(scaleValues, 31, 36);
        int domain6 = sumRange(scaleValues, 37, 40);

        int totalScore = domain1 + domain2 + domain3 + domain4 + domain5 + domain6;

        SeverityLevel severity = classifySeverity(totalScore);
        int disabilityPct = computeDisabilityPercentage(totalScore);

        IsaaScore score = IsaaScore.builder()
                .sessionId(sessionId)
                .totalScore(totalScore)
                .severity(severity)
                .disabilityPct(disabilityPct)
                .domain1Social(domain1)
                .domain2Emotional(domain2)
                .domain3Speech(domain3)
                .domain4Behaviour(domain4)
                .domain5Sensory(domain5)
                .domain6Cognitive(domain6)
                .build();

        score = isaaScoreRepository.save(score);
        auditLog.logScoreComputed(sessionId, totalScore);

        return toDTO(score);
    }

    public IsaaScoreDTO getScore(UUID sessionId, UUID userId, String userRole) {
        AssessmentSession session = sessionService.findById(sessionId);
        sessionService.checkAccess(session, userId, userRole);

        IsaaScore score = isaaScoreRepository.findBySessionId(sessionId)
                .orElseThrow(() -> AppException.notFound(ErrorCode.SCORE_NOT_FOUND, "Score not found for this session"));
        return toDTO(score);
    }

    /**
     * Severity classification (total score):
     * Below 70: NO_AUTISM
     * 70 to 106 inclusive: MILD
     * 107 to 153 inclusive: MODERATE
     * Above 153: SEVERE
     */
    static SeverityLevel classifySeverity(int totalScore) {
        if (totalScore < 70) return SeverityLevel.NO_AUTISM;
        if (totalScore <= 106) return SeverityLevel.MILD;
        if (totalScore <= 153) return SeverityLevel.MODERATE;
        return SeverityLevel.SEVERE;
    }

    /**
     * Disability percentage mapping (exact NIMH table):
     * Score below 70: 0%
     * Score exactly 70: 40%
     * Score 71–88: 50%
     * Score 89–105: 60%
     * Score 106–123: 70%
     * Score 124–140: 80%
     * Score 141–158: 90%
     * Score above 158: 100%
     */
    static int computeDisabilityPercentage(int totalScore) {
        if (totalScore < 70) return 0;
        if (totalScore == 70) return 40;
        if (totalScore <= 88) return 50;
        if (totalScore <= 105) return 60;
        if (totalScore <= 123) return 70;
        if (totalScore <= 140) return 80;
        if (totalScore <= 158) return 90;
        return 100;
    }

    private int sumRange(int[] values, int from, int to) {
        int sum = 0;
        for (int i = from; i <= to; i++) {
            sum += values[i];
        }
        return sum;
    }

    private IsaaScoreDTO toDTO(IsaaScore score) {
        return IsaaScoreDTO.builder()
                .id(score.getId())
                .sessionId(score.getSessionId())
                .totalScore(score.getTotalScore())
                .severity(score.getSeverity().name())
                .disabilityPct(score.getDisabilityPct())
                .domain1Social(score.getDomain1Social())
                .domain2Emotional(score.getDomain2Emotional())
                .domain3Speech(score.getDomain3Speech())
                .domain4Behaviour(score.getDomain4Behaviour())
                .domain5Sensory(score.getDomain5Sensory())
                .domain6Cognitive(score.getDomain6Cognitive())
                .scoredAt(score.getScoredAt())
                .build();
    }
}
