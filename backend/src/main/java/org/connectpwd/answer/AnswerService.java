package org.connectpwd.answer;

import lombok.RequiredArgsConstructor;
import org.connectpwd.answer.dto.AnswerResponse;
import org.connectpwd.answer.dto.TextAnswerRequest;
import org.connectpwd.common.AppException;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.question.ModuleQuestion;
import org.connectpwd.question.ModuleQuestionBank;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.QuestionItem;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionService;
import org.connectpwd.session.SessionStatus;
import org.connectpwd.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AnswerService {

    private final ResponseRepository responseRepository;
    private final SessionService sessionService;
    private final QuestionBank questionBank;
    private final ModuleQuestionBank moduleQuestionBank;
    private final StorageService storageService;

    public AnswerResponse submitTextAnswer(String userId, TextAnswerRequest request) {
        AssessmentSession session = sessionService.findById(request.getSessionId());
        sessionService.checkAccess(session, userId, null);
        validateSessionActive(session);

        if (responseRepository.findBySessionIdAndQuestionCode(
                session.getId(), request.getQuestionCode()).isPresent()) {
            throw AppException.conflict(ErrorCode.ANSWER_ALREADY_EXISTS, "Answer already submitted for this question");
        }

        ResponseDocument doc;
        if (session.getModuleType() != null) {
            ModuleQuestion question = moduleQuestionBank.findById(session.getModuleType(), request.getQuestionCode());
            if (question == null) {
                throw AppException.notFound(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + request.getQuestionCode());
            }
            doc = ResponseDocument.builder()
                    .sessionId(session.getId())
                    .level(1)
                    .questionIndex(question.getFlatIndex())
                    .questionCode(request.getQuestionCode())
                    .domain(question.getSectionTitle())
                    .questionText(question.getText())
                    .answerType(request.getAnswerType())
                    .answerText(request.getAnswerText())
                    .scaleValue(request.getScaleValue())
                    .build();
        } else {
            QuestionItem question = questionBank.findByCode(session.getCurrentLevel(), request.getQuestionCode());
            if (question == null) {
                throw AppException.notFound(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + request.getQuestionCode());
            }
            int questionIndex = questionBank.findIndexByCode(session.getCurrentLevel(), request.getQuestionCode());
            boolean isHindi = "hi".equals(session.getLanguage());
            doc = ResponseDocument.builder()
                    .sessionId(session.getId())
                    .level(session.getCurrentLevel())
                    .questionIndex(questionIndex)
                    .questionCode(request.getQuestionCode())
                    .domain(isHindi ? question.getDomainNameHi() : question.getDomainNameEn())
                    .questionText(isHindi ? question.getTextHi() : question.getTextEn())
                    .answerType(request.getAnswerType())
                    .answerText(request.getAnswerText())
                    .scaleValue(request.getScaleValue())
                    .build();
        }

        responseRepository.save(doc);
        sessionService.advanceQuestion(session);

        return buildAnswerResponse(session);
    }

    public AnswerResponse submitVoiceAnswer(String userId, String sessionId, String questionCode, MultipartFile audio, String transcript) {
        AssessmentSession session = sessionService.findById(sessionId);
        sessionService.checkAccess(session, userId, null);
        validateSessionActive(session);

        if (responseRepository.findBySessionIdAndQuestionCode(
                session.getId(), questionCode).isPresent()) {
            throw AppException.conflict(ErrorCode.ANSWER_ALREADY_EXISTS, "Answer already submitted for this question");
        }

        String audioKey = storageService.uploadVoice(sessionId, questionCode, audio);
        ResponseDocument doc;

        if (session.getModuleType() != null) {
            ModuleQuestion question = moduleQuestionBank.findById(session.getModuleType(), questionCode);
            if (question == null) {
                throw AppException.notFound(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionCode);
            }
            doc = ResponseDocument.builder()
                    .sessionId(session.getId())
                    .level(1)
                    .questionIndex(question.getFlatIndex())
                    .questionCode(questionCode)
                    .domain(question.getSectionTitle())
                    .questionText(question.getText())
                    .answerType("VOICE")
                    .audioKey(audioKey)
                    .transcript(transcript)
                    .answerText(transcript)
                    .build();
        } else {
            QuestionItem question = questionBank.findByCode(session.getCurrentLevel(), questionCode);
            if (question == null) {
                throw AppException.notFound(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionCode);
            }
            int questionIndex = questionBank.findIndexByCode(session.getCurrentLevel(), questionCode);
            boolean isHindi = "hi".equals(session.getLanguage());
            doc = ResponseDocument.builder()
                    .sessionId(session.getId())
                    .level(session.getCurrentLevel())
                    .questionIndex(questionIndex)
                    .questionCode(questionCode)
                    .domain(isHindi ? question.getDomainNameHi() : question.getDomainNameEn())
                    .questionText(isHindi ? question.getTextHi() : question.getTextEn())
                    .answerType("VOICE")
                    .audioKey(audioKey)
                    .transcript(transcript)
                    .answerText(transcript)
                    .build();
        }

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
        session = sessionService.findById(session.getId());

        boolean completed = session.getStatus() == SessionStatus.COMPLETED;
        boolean levelComplete = session.getStatus() == SessionStatus.LEVEL_COMPLETE;
        QuestionDTO nextQuestion = null;
        Integer nextLevel = null;

        if (completed) {
            // No next question
        } else if (levelComplete) {
            nextLevel = session.getCurrentLevel() + 1;
            if (nextLevel > 4) nextLevel = null;
        } else if (session.getModuleType() != null) {
            nextQuestion = moduleQuestionBank.toDTO(
                    session.getModuleType(),
                    session.getCurrentQuestionIndex()
            );
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