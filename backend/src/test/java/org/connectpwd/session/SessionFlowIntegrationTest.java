package org.connectpwd.session;

import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.ModuleQuestionBank;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.dto.SessionResponse;
import org.connectpwd.session.dto.StartSessionRequest;
import org.connectpwd.session.dto.TriageRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class SessionFlowIntegrationTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private QuestionBank questionBank;
    @Mock private ModuleQuestionBank moduleQuestionBank;
    @Mock private AuditLog auditLog;

    @InjectMocks
    private SessionService sessionService;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user-id-001";
    }

    @Test
    void startSession_success_returnsFirstQuestion() {
        StartSessionRequest request = new StartSessionRequest();
        request.setModuleType("PARENT");
        request.setLanguage("en");
        TriageRequestDTO triage = new TriageRequestDTO();
        triage.setSeekingFor("child");
        triage.setAge(8);
        triage.setChallengeType("ASD");
        request.setTriageData(triage);

        AssessmentSession savedSession = AssessmentSession.builder()
                .id("session-id-001")
                .userId(userId)
                .moduleType("PARENT")
                .currentLevel(1)
                .currentQuestionIndex(0)
                .language("en")
                .status(SessionStatus.IN_PROGRESS)
                .build();
        when(sessionRepository.save(any(AssessmentSession.class))).thenReturn(savedSession);

        QuestionDTO firstQ = QuestionDTO.builder()
                .code("L1_1")
                .text("What is the child's full name?")
                .build();
        when(moduleQuestionBank.toDTO("PARENT", 0)).thenReturn(firstQ);
        when(moduleQuestionBank.getTotalQuestions("PARENT")).thenReturn(50);

        SessionResponse response = sessionService.startSession(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentLevel()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getCurrentQuestion().getCode()).isEqualTo("L1_1");
        verify(sessionRepository).save(any(AssessmentSession.class));
        verify(auditLog).logSessionStart(eq(userId), anyString());
    }

    @Test
    void startSession_nullTriageData_throwsException() {
        StartSessionRequest request = new StartSessionRequest();
        request.setModuleType("PARENT");
        request.setLanguage("en");
        // triageData is null — should throw NullPointerException or AppException
        assertThatThrownBy(() -> sessionService.startSession(userId, request))
                .isInstanceOf(Exception.class);
    }

    @Test
    void checkAccess_psychologist_canAccessAnySession() {
        AssessmentSession session = AssessmentSession.builder()
                .id("session-id-002")
                .userId("other-user-id") // different user
                .build();

        assertThatCode(() -> sessionService.checkAccess(session, userId, "PSYCHOLOGIST"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAccess_admin_canAccessAnySession() {
        AssessmentSession session = AssessmentSession.builder()
                .id("session-id-003")
                .userId("another-user-id")
                .build();

        assertThatCode(() -> sessionService.checkAccess(session, userId, "ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAccess_caregiver_cannotAccessOtherSession() {
        AssessmentSession session = AssessmentSession.builder()
                .id("session-id-004")
                .userId("yet-another-user") // different user
                .build();

        assertThatThrownBy(() -> sessionService.checkAccess(session, userId, "CAREGIVER"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("You do not have access");
    }

    @Test
    void advanceQuestion_incrementsIndex() {
        AssessmentSession session = AssessmentSession.builder()
                .id("session-id-005")
                .userId(userId)
                .currentLevel(1)
                .currentQuestionIndex(2)
                .status(SessionStatus.IN_PROGRESS)
                .build();

        when(questionBank.getLevelSize(1)).thenReturn(10);

        sessionService.advanceQuestion(session);

        assertThat(session.getCurrentQuestionIndex()).isEqualTo(3);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void advanceQuestion_lastQuestion_setsLevelComplete() {
        AssessmentSession session = AssessmentSession.builder()
                .id("session-id-006")
                .userId(userId)
                .currentLevel(1)
                .currentQuestionIndex(9)
                .status(SessionStatus.IN_PROGRESS)
                .build();

        when(questionBank.getLevelSize(1)).thenReturn(10);

        sessionService.advanceQuestion(session);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.LEVEL_COMPLETE);
    }
}
