package org.connectpwd.answer;

import lombok.RequiredArgsConstructor;
import org.connectpwd.answer.dto.AnswerResponse;
import org.connectpwd.answer.dto.TextAnswerRequest;
import org.connectpwd.common.AppException;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.QuestionItem;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.connectpwd.session.SessionStatus;
import org.connectpwd.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final ResponseRepository responseRepository;
    private final SessionService sessionService;
    private final QuestionBank questionBank;
    private final StorageService storageService;

    public AnswerResponse submitTextAnswer(UUID userId, TextAnswerRequest request) {
        AssessmentSession session = sessionService.findById(request.getSessionId());
        sessionService.checkAccess(session, userId, null);
        validateSessionActive(session);

        QuestionItem question = questionBank.findByCode(session.getCurrentLevel(), request.getQuestionCode());
        if (question == null) {
            throw AppException.notFound(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + request.getQuestionCode());
        }

        if (responseRepository.findBySessionIdAndQuestionCode(
                session.getId().toString(), request.getQuestionCode()).isPresent()) {
            throw AppException.conflict(ErrorCode.ANSWER_ALREADY_EXISTS, "Answer already submitted for this question");
        }

        int questionIndex = questionBank.findIndexByCode(session.getCurrentLevel(), request.getQuestionCode());
        boolean isHindi = "hi".equals(session.getLanguage());

        ResponseDocument doc = ResponseDocument.builder()
                .sessionId(session.getId().toString())
                .level(session.getCurrentLevel())
                .questionIndex(questionIndex)
                .questionCode(request.getQuestionCode())
                .domain(isHindi ? question.getDomainNameHi() : question.getDomainNameEn())
                .questionText(isHindi ? question.getTextHi() : question.getTextEn())
                .answerType(request.getAnswerType())
                .answerText(request.getAnswerText())
                .scaleValue(request.getScaleValue())
                .build();

        responseRepository.save(doc);
        sessionService.advanceQuestion(session);

        return buildAnswerResponse(session);
    }

    public AnswerResponse submitVoiceAnswer(UUID userId, UUID sessionId, String questionCode, MultipartFile audio) {
        AssessmentSession session = sessionService.findById(sessionId);
        sessionService.checkAccess(session, userId, null);
        validateSessionActive(session);

        QuestionItem question = questionBank.findByCode(session.getCurrentLevel(), questionCode);
        if (question == null) {
            throw AppException.notFound(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionCode);
        }

        if (responseRepository.findBySessionIdAndQuestionCode(
                session.getId().toString(), questionCode).isPresent()) {
            throw AppException.conflict(ErrorCode.ANSWER_ALREADY_EXISTS, "Answer already submitted for this question");
        }

        String audioKey = storageService.uploadVoice(sessionId, questionCode, audio);
        int questionIndex = questionBank.findIndexByCode(session.getCurrentLevel(), questionCode);
        boolean isHindi = "hi".equals(session.getLanguage());

        ResponseDocument doc = ResponseDocument.builder()
                .sessionId(session.getId().toString())
                .level(session.getCurrentLevel())
                .questionIndex(questionIndex)
                .questionCode(questionCode)
                .domain(isHindi ? question.getDomainNameHi() : question.getDomainNameEn())
                .questionText(isHindi ? question.getTextHi() : question.getTextEn())
                .answerType("VOICE")
                .audioKey(audioKey)
                .build();

        responseRepository.save(doc);
        sessionService.advanceQuestion(session);

        return buildAnswerResponse(session);
    }

    private void validateSessionActive(AssessmentSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.ABANDONED) {
            throw AppException.badRequest(ErrorCode.SESSION_ALREADY_COMPLETED, "Session is no longer active");
        }
    }

    private AnswerResponse buildAnswerResponse(AssessmentSession session) {
        // Reload session after advance
        session = sessionService.findById(session.getId());

        boolean levelComplete = session.getStatus() == SessionStatus.LEVEL_COMPLETE;
        QuestionDTO nextQuestion = null;
        Integer nextLevel = null;

        if (levelComplete) {
            nextLevel = session.getCurrentLevel() + 1;
            if (nextLevel > 4) nextLevel = null;
        } else {
            nextQuestion = questionBank.toDTO(
                    session.getCurrentLevel(),
                    session.getCurrentQuestionIndex(),
                    session.getLanguage()
            );
        }

        return AnswerResponse.builder()
                .nextQuestion(nextQuestion)
                .levelComplete(levelComplete)
                .nextLevel(nextLevel)
                .sessionStatus(session.getStatus().name())
                .build();
    }
}
