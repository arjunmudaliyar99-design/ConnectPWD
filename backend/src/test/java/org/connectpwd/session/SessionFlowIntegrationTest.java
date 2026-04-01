package org.connectpwd.session;

import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.consent.Consent;
import org.connectpwd.consent.ConsentService;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.dto.SessionResponse;
import org.connectpwd.session.dto.StartSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionFlowIntegrationTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private ConsentService consentService;
    @Mock private QuestionBank questionBank;
    @Mock private AuditLog auditLog;

    @InjectMocks
    private SessionService sessionService;

    private UUID userId;
    private UUID consentId;
    private Consent consent;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        consentId = UUID.randomUUID();

        consent = Consent.builder()
                .id(consentId)
                .userId(userId)
                .clientName("Test Child")
                .legalName("Test Parent")
                .agreed(true)
                .build();
    }

    @Test
    void startSession_success_returnsFirstQuestion() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConsentId(consentId);
        request.setLanguage("en");

        when(consentService.findById(consentId)).thenReturn(consent);
        when(sessionRepository.existsByConsentId(consentId)).thenReturn(false);

        AssessmentSession savedSession = AssessmentSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentId(consentId)
                .currentLevel(1)
                .currentQuestionIndex(0)
                .language("en")
                .status(SessionStatus.IN_PROGRESS)
                .build();
        when(sessionRepository.save(any(AssessmentSession.class))).thenReturn(savedSession);

        QuestionDTO firstQ = QuestionDTO.builder()
                .code("L1_1")
                .textEn("What is the child's full name?")
                .build();
        when(questionBank.toDTO(1, 0, "en")).thenReturn(firstQ);

        SessionResponse response = sessionService.startSession(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentLevel()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getCurrentQuestion().getCode()).isEqualTo("L1_1");
        verify(sessionRepository).save(any(AssessmentSession.class));
        verify(auditLog).logSessionStart(eq(userId), any(UUID.class));
    }

    @Test
    void startSession_consentNotAgreed_throwsException() {
        consent.setAgreed(false);
        StartSessionRequest request = new StartSessionRequest();
        request.setConsentId(consentId);

        when(consentService.findById(consentId)).thenReturn(consent);

        assertThatThrownBy(() -> sessionService.startSession(userId, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Consent has not been agreed");
    }

    @Test
    void startSession_consentNotFound_throwsException() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConsentId(consentId);

        when(consentService.findById(consentId)).thenReturn(null);

        assertThatThrownBy(() -> sessionService.startSession(userId, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Consent record not found");
    }

    @Test
    void startSession_duplicateConsent_throwsConflict() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConsentId(consentId);

        when(consentService.findById(consentId)).thenReturn(consent);
        when(sessionRepository.existsByConsentId(consentId)).thenReturn(true);

        assertThatThrownBy(() -> sessionService.startSession(userId, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("A session already exists");
    }

    @Test
    void startSession_wrongUser_throwsForbidden() {
        UUID otherUserId = UUID.randomUUID();
        StartSessionRequest request = new StartSessionRequest();
        request.setConsentId(consentId);

        when(consentService.findById(consentId)).thenReturn(consent);

        assertThatThrownBy(() -> sessionService.startSession(otherUserId, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Consent does not belong to this user");
    }

    @Test
    void checkAccess_psychologist_canAccessAnySession() {
        AssessmentSession session = AssessmentSession.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID()) // different user
                .build();

        assertThatCode(() -> sessionService.checkAccess(session, userId, "PSYCHOLOGIST"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAccess_admin_canAccessAnySession() {
        AssessmentSession session = AssessmentSession.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();

        assertThatCode(() -> sessionService.checkAccess(session, userId, "ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAccess_caregiver_cannotAccessOtherSession() {
        AssessmentSession session = AssessmentSession.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID()) // different user
                .build();

        assertThatThrownBy(() -> sessionService.checkAccess(session, userId, "CAREGIVER"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("You do not have access");
    }

    @Test
    void advanceQuestion_incrementsIndex() {
        AssessmentSession session = AssessmentSession.builder()
                .id(UUID.randomUUID())
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
                .id(UUID.randomUUID())
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
