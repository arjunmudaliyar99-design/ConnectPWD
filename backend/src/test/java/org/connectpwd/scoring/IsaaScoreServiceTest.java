package org.connectpwd.scoring;

import org.connectpwd.answer.ResponseDocument;
import org.connectpwd.answer.ResponseRepository;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.scoring.dto.IsaaScoreDTO;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsaaScoreServiceTest {

    @Mock private ResponseRepository responseRepository;
    @Mock private IsaaScoreRepository isaaScoreRepository;
    @Mock private SessionService sessionService;
    @Mock private AuditLog auditLog;

    @InjectMocks
    private IsaaScoreService isaaScoreService;

    private UUID sessionId;
    private UUID userId;
    private AssessmentSession session;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        session = AssessmentSession.builder()
                .id(sessionId)
                .userId(userId)
                .build();
    }

    @Test
    void computeScore_returnsExistingScore() {
        IsaaScore existing = IsaaScore.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .totalScore(120)
                .severity(SeverityLevel.MODERATE)
                .disabilityPct(70)
                .domain1Social(20)
                .domain2Emotional(10)
                .domain3Speech(25)
                .domain4Behaviour(20)
                .domain5Sensory(25)
                .domain6Cognitive(20)
                .build();

        when(sessionService.findById(sessionId)).thenReturn(session);
        when(isaaScoreRepository.existsBySessionId(sessionId)).thenReturn(true);
        when(isaaScoreRepository.findBySessionId(sessionId)).thenReturn(Optional.of(existing));

        IsaaScoreDTO result = isaaScoreService.computeScore(sessionId, userId, "CAREGIVER");

        assertThat(result.getTotalScore()).isEqualTo(120);
        assertThat(result.getSeverity()).isEqualTo("MODERATE");
        verify(responseRepository, never()).findBySessionIdAndLevel(any(), anyInt());
    }

    @Test
    void computeScore_all40Items_computesCorrectly() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(isaaScoreRepository.existsBySessionId(sessionId)).thenReturn(false);

        // Create 40 responses, all with scaleValue = 3 → total = 120
        List<ResponseDocument> responses = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ResponseDocument doc = ResponseDocument.builder()
                    .sessionId(sessionId.toString())
                    .level(2)
                    .questionIndex(i)
                    .questionCode("L2_" + (i + 1))
                    .scaleValue(3)
                    .build();
            responses.add(doc);
        }

        when(responseRepository.findBySessionIdAndLevel(sessionId.toString(), 2)).thenReturn(responses);
        when(isaaScoreRepository.save(any(IsaaScore.class))).thenAnswer(inv -> {
            IsaaScore s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        IsaaScoreDTO result = isaaScoreService.computeScore(sessionId, userId, "CAREGIVER");

        // All 40 items × 3 = 120
        assertThat(result.getTotalScore()).isEqualTo(120);
        assertThat(result.getSeverity()).isEqualTo("MODERATE");
        assertThat(result.getDisabilityPct()).isEqualTo(70);

        // Domain breakdowns: items 1-9 = 27, 10-14 = 15, 15-23 = 27, 24-30 = 21, 31-36 = 18, 37-40 = 12
        assertThat(result.getDomain1Social()).isEqualTo(27);
        assertThat(result.getDomain2Emotional()).isEqualTo(15);
        assertThat(result.getDomain3Speech()).isEqualTo(27);
        assertThat(result.getDomain4Behaviour()).isEqualTo(21);
        assertThat(result.getDomain5Sensory()).isEqualTo(18);
        assertThat(result.getDomain6Cognitive()).isEqualTo(12);
    }

    @Test
    void computeScore_lessThan40Items_throwsException() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(isaaScoreRepository.existsBySessionId(sessionId)).thenReturn(false);

        List<ResponseDocument> responses = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            responses.add(ResponseDocument.builder()
                    .sessionId(sessionId.toString())
                    .level(2)
                    .questionIndex(i)
                    .scaleValue(3)
                    .build());
        }
        when(responseRepository.findBySessionIdAndLevel(sessionId.toString(), 2)).thenReturn(responses);

        assertThatThrownBy(() -> isaaScoreService.computeScore(sessionId, userId, "CAREGIVER"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("All 40 ISAA items must be answered");
    }

    @ParameterizedTest
    @CsvSource({
            "50, NO_AUTISM",
            "69, NO_AUTISM",
            "70, MILD",
            "90, MILD",
            "106, MILD",
            "107, MODERATE",
            "130, MODERATE",
            "153, MODERATE",
            "154, SEVERE",
            "200, SEVERE"
    })
    void classifySeverity_correctBoundaries(int score, String expected) {
        assertThat(IsaaScoreService.classifySeverity(score).name()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "40, 0",
            "69, 0",
            "70, 40",
            "71, 50",
            "88, 50",
            "89, 60",
            "105, 60",
            "106, 70",
            "123, 70",
            "124, 80",
            "140, 80",
            "141, 90",
            "158, 90",
            "159, 100",
            "200, 100"
    })
    void computeDisabilityPercentage_matchesNIMHTable(int score, int expectedPct) {
        assertThat(IsaaScoreService.computeDisabilityPercentage(score)).isEqualTo(expectedPct);
    }

    @Test
    void getScore_notFound_throwsException() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(isaaScoreRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> isaaScoreService.getScore(sessionId, userId, "CAREGIVER"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Score not found");
    }
}
