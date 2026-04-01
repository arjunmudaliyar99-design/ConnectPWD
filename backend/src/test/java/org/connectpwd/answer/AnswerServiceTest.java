package org.connectpwd.answer;

import org.connectpwd.answer.dto.AnswerResponse;
import org.connectpwd.answer.dto.TextAnswerRequest;
import org.connectpwd.common.AppException;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.QuestionItem;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.connectpwd.session.SessionStatus;
import org.connectpwd.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock private ResponseRepository responseRepository;
    @Mock private SessionService sessionService;
    @Mock private QuestionBank questionBank;
    @Mock private StorageService storageService;

    @InjectMocks
    private AnswerService answerService;

    private UUID userId;
    private UUID sessionId;
    private AssessmentSession session;
    private TextAnswerRequest textRequest;
    private QuestionItem questionItem;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        session = AssessmentSession.builder()
                .id(sessionId)
                .userId(userId)
                .currentLevel(2)
                .currentQuestionIndex(0)
                .language("en")
                .status(SessionStatus.IN_PROGRESS)
                .build();

        textRequest = new TextAnswerRequest();
        textRequest.setSessionId(sessionId);
        textRequest.setQuestionCode("L2_1");
        textRequest.setAnswerType("SCALE");
        textRequest.setScaleValue(3);

        questionItem = new QuestionItem();
        questionItem.setCode("L2_1");
        questionItem.setDomainNameEn("Social Relationship & Reciprocity");
        questionItem.setDomainNameHi("सामाजिक संबंध और पारस्परिकता");
        questionItem.setTextEn("Does the child enjoy being with other children?");
        questionItem.setTextHi("क्या बच्चा अन्य बच्चों के साथ रहना पसंद करता है?");
    }

    @Test
    void submitTextAnswer_success_returnsNextQuestion() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(questionBank.findByCode(2, "L2_1")).thenReturn(questionItem);
        when(questionBank.findIndexByCode(2, "L2_1")).thenReturn(0);
        when(responseRepository.findBySessionIdAndQuestionCode(sessionId.toString(), "L2_1"))
                .thenReturn(Optional.empty());
        when(responseRepository.save(any(ResponseDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        // After advance, session index moves to 1
        AssessmentSession advancedSession = AssessmentSession.builder()
                .id(sessionId)
                .userId(userId)
                .currentLevel(2)
                .currentQuestionIndex(1)
                .language("en")
                .status(SessionStatus.IN_PROGRESS)
                .build();
        when(sessionService.findById(sessionId))
                .thenReturn(session)       // first call
                .thenReturn(advancedSession); // second call (after advance)

        QuestionDTO nextQ = QuestionDTO.builder()
                .code("L2_2")
                .textEn("Test question 2")
                .build();
        when(questionBank.toDTO(2, 1, "en")).thenReturn(nextQ);

        AnswerResponse response = answerService.submitTextAnswer(userId, textRequest);

        assertThat(response.isLevelComplete()).isFalse();
        assertThat(response.getNextQuestion()).isNotNull();
        assertThat(response.getNextQuestion().getCode()).isEqualTo("L2_2");
        verify(responseRepository).save(any(ResponseDocument.class));
        verify(sessionService).advanceQuestion(session);
    }

    @Test
    void submitTextAnswer_completedSession_throwsException() {
        session.setStatus(SessionStatus.COMPLETED);
        when(sessionService.findById(sessionId)).thenReturn(session);

        assertThatThrownBy(() -> answerService.submitTextAnswer(userId, textRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Session is no longer active");
    }

    @Test
    void submitTextAnswer_duplicateAnswer_throwsConflict() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(questionBank.findByCode(2, "L2_1")).thenReturn(questionItem);
        when(responseRepository.findBySessionIdAndQuestionCode(sessionId.toString(), "L2_1"))
                .thenReturn(Optional.of(new ResponseDocument()));

        assertThatThrownBy(() -> answerService.submitTextAnswer(userId, textRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Answer already submitted");
    }

    @Test
    void submitTextAnswer_questionNotFound_throwsNotFound() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(questionBank.findByCode(2, "L2_1")).thenReturn(null);

        assertThatThrownBy(() -> answerService.submitTextAnswer(userId, textRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Question not found");
    }

    @Test
    void submitTextAnswer_levelComplete_returnsNextLevel() {
        when(sessionService.findById(sessionId)).thenReturn(session);
        when(questionBank.findByCode(2, "L2_1")).thenReturn(questionItem);
        when(questionBank.findIndexByCode(2, "L2_1")).thenReturn(0);
        when(responseRepository.findBySessionIdAndQuestionCode(sessionId.toString(), "L2_1"))
                .thenReturn(Optional.empty());
        when(responseRepository.save(any(ResponseDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentSession levelCompleteSession = AssessmentSession.builder()
                .id(sessionId)
                .userId(userId)
                .currentLevel(2)
                .currentQuestionIndex(39)
                .language("en")
                .status(SessionStatus.LEVEL_COMPLETE)
                .build();
        when(sessionService.findById(sessionId))
                .thenReturn(session)
                .thenReturn(levelCompleteSession);

        AnswerResponse response = answerService.submitTextAnswer(userId, textRequest);

        assertThat(response.isLevelComplete()).isTrue();
        assertThat(response.getNextLevel()).isEqualTo(3);
        assertThat(response.getNextQuestion()).isNull();
    }
}
