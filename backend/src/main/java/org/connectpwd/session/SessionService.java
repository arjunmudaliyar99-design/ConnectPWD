package org.connectpwd.session;

import lombok.RequiredArgsConstructor;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.question.ModuleQuestionBank;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.dto.SessionResponse;
import org.connectpwd.session.dto.StartSessionRequest;
import org.connectpwd.session.dto.TriageRequestDTO;
import org.connectpwd.user.UserRole;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SessionService {

    private final SessionRepository sessionRepository;
    private final QuestionBank questionBank;
    private final ModuleQuestionBank moduleQuestionBank;
    private final AuditLog auditLog;

    public SessionResponse startSession(String userId, StartSessionRequest request) {
        String moduleType = request.getModuleType();
        TriageRequestDTO triage = request.getTriageData();
        String language = request.getLanguage() != null ? request.getLanguage() : "en";

        AssessmentSession session = AssessmentSession.builder()
                .userId(userId)
                .moduleType(moduleType)
                .triageSeekingFor(triage.getSeekingFor())
                .triageAge(triage.getAge())
                .triageChallengeType(triage.getChallengeType())
                .currentLevel(1)
                .currentQuestionIndex(0)
                .language(language)
                .build();

        session = sessionRepository.save(session);
        auditLog.logSessionStart(userId, session.getId());

        QuestionDTO firstQuestion = moduleQuestionBank.toDTO(moduleType, 0);
        int total = moduleQuestionBank.getTotalQuestions(moduleType);

        return toResponse(session, firstQuestion, total);
    }

    public SessionResponse getSession(String sessionId, String userId, String userRole) {
        AssessmentSession session = findById(sessionId);
        checkAccess(session, userId, userRole);

        QuestionDTO currentQuestion = resolveCurrentQuestion(session);
        int total = resolveTotalQuestions(session);

        return toResponse(session, currentQuestion, total);
    }

    public AssessmentSession findById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound(ErrorCode.SESSION_NOT_FOUND, "Session not found"));
    }

    public void checkAccess(AssessmentSession session, String userId, String userRole) {
        if (userRole != null && (UserRole.PSYCHOLOGIST.name().equals(userRole) || UserRole.ADMIN.name().equals(userRole))) {
            return;
        }
        if (!session.getUserId().equals(userId)) {
            throw AppException.forbidden(ErrorCode.SESSION_ACCESS_DENIED, "You do not have access to this session");
        }
    }

    public void advanceQuestion(AssessmentSession session) {
        int nextIndex = session.getCurrentQuestionIndex() + 1;
        int total = resolveTotalQuestions(session);

        if (nextIndex >= total) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            auditLog.logSessionComplete(session.getId());
        } else {
            session.setCurrentQuestionIndex(nextIndex);
        }

        sessionRepository.save(session);
    }

    public SessionResponse advanceLevel(String sessionId, int nextLevel, String userId, String userRole) {
        AssessmentSession session = findById(sessionId);
        checkAccess(session, userId, userRole);

        if (session.getModuleType() != null) {
            QuestionDTO currentQuestion = resolveCurrentQuestion(session);
            int total = resolveTotalQuestions(session);
            return toResponse(session, currentQuestion, total);
        }

        advanceToLevel(session, nextLevel);
        QuestionDTO currentQuestion = null;
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            currentQuestion = questionBank.toDTO(
                    session.getCurrentLevel(),
                    session.getCurrentQuestionIndex(),
                    session.getLanguage()
            );
        }
        return toResponse(session, currentQuestion, questionBank.getLevelSize(session.getCurrentLevel()));
    }

    public void advanceToLevel(AssessmentSession session, int nextLevel) {
        if (nextLevel > 4) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            auditLog.logSessionComplete(session.getId());
        } else {
            session.setCurrentLevel(nextLevel);
            session.setCurrentQuestionIndex(0);
            session.setStatus(SessionStatus.IN_PROGRESS);
        }
        sessionRepository.save(session);
    }

    private QuestionDTO resolveCurrentQuestion(AssessmentSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return null;
        }
        if (session.getModuleType() != null) {
            return moduleQuestionBank.toDTO(session.getModuleType(), session.getCurrentQuestionIndex());
        }
        return questionBank.toDTO(session.getCurrentLevel(), session.getCurrentQuestionIndex(), session.getLanguage());
    }

    private int resolveTotalQuestions(AssessmentSession session) {
        if (session.getModuleType() != null) {
            return moduleQuestionBank.getTotalQuestions(session.getModuleType());
        }
        return questionBank.getLevelSize(session.getCurrentLevel());
    }

    private SessionResponse toResponse(AssessmentSession session, QuestionDTO currentQuestion, int totalQuestions) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .moduleType(session.getModuleType())
                .currentLevel(session.getCurrentLevel())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .totalQuestions(totalQuestions)
                .status(session.getStatus().name())
                .language(session.getLanguage())
                .startedAt(session.getStartedAt())
                .currentQuestion(currentQuestion)
                .build();
    }
}
