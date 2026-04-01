package org.connectpwd.session;

import lombok.RequiredArgsConstructor;
import org.connectpwd.common.AppException;
import org.connectpwd.common.AuditLog;
import org.connectpwd.common.ErrorCode;
import org.connectpwd.consent.Consent;
import org.connectpwd.consent.ConsentService;
import org.connectpwd.question.QuestionBank;
import org.connectpwd.question.dto.QuestionDTO;
import org.connectpwd.session.dto.SessionResponse;
import org.connectpwd.session.dto.StartSessionRequest;
import org.connectpwd.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ConsentService consentService;
    private final QuestionBank questionBank;
    private final AuditLog auditLog;

    @Transactional
    public SessionResponse startSession(UUID userId, StartSessionRequest request) {
        Consent consent = consentService.findById(request.getConsentId());
        if (consent == null) {
            throw AppException.notFound(ErrorCode.CONSENT_NOT_FOUND, "Consent record not found");
        }
        if (!consent.isAgreed()) {
            throw AppException.badRequest(ErrorCode.CONSENT_NOT_AGREED, "Consent has not been agreed");
        }
        if (!consent.getUserId().equals(userId)) {
            throw AppException.forbidden(ErrorCode.SESSION_ACCESS_DENIED, "Consent does not belong to this user");
        }
        if (sessionRepository.existsByConsentId(consent.getId())) {
            throw AppException.conflict(ErrorCode.SESSION_ALREADY_COMPLETED, "A session already exists for this consent");
        }

        String language = request.getLanguage() != null ? request.getLanguage() : "en";

        AssessmentSession session = AssessmentSession.builder()
                .userId(userId)
                .consentId(consent.getId())
                .currentLevel(1)
                .currentQuestionIndex(0)
                .language(language)
                .build();

        session = sessionRepository.save(session);
        auditLog.logSessionStart(userId, session.getId());

        QuestionDTO firstQuestion = questionBank.toDTO(1, 0, language);

        return toResponse(session, firstQuestion);
    }

    public SessionResponse getSession(UUID sessionId, UUID userId, String userRole) {
        AssessmentSession session = findById(sessionId);
        checkAccess(session, userId, userRole);

        QuestionDTO currentQuestion = questionBank.toDTO(
                session.getCurrentLevel(),
                session.getCurrentQuestionIndex(),
                session.getLanguage()
        );

        return toResponse(session, currentQuestion);
    }

    public AssessmentSession findById(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound(ErrorCode.SESSION_NOT_FOUND, "Session not found"));
    }

    public void checkAccess(AssessmentSession session, UUID userId, String userRole) {
        if (UserRole.PSYCHOLOGIST.name().equals(userRole) || UserRole.ADMIN.name().equals(userRole)) {
            return;
        }
        if (!session.getUserId().equals(userId)) {
            throw AppException.forbidden(ErrorCode.SESSION_ACCESS_DENIED, "You do not have access to this session");
        }
    }

    @Transactional
    public void advanceQuestion(AssessmentSession session) {
        int nextIndex = session.getCurrentQuestionIndex() + 1;
        int levelSize = questionBank.getLevelSize(session.getCurrentLevel());

        if (nextIndex >= levelSize) {
            session.setStatus(SessionStatus.LEVEL_COMPLETE);
            session.setCurrentQuestionIndex(levelSize - 1);
        } else {
            session.setCurrentQuestionIndex(nextIndex);
        }

        sessionRepository.save(session);
    }

    @Transactional
    public SessionResponse advanceLevel(UUID sessionId, int nextLevel, UUID userId, String userRole) {
        AssessmentSession session = findById(sessionId);
        checkAccess(session, userId, userRole);
        advanceToLevel(session, nextLevel);

        QuestionDTO currentQuestion = null;
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            currentQuestion = questionBank.toDTO(
                    session.getCurrentLevel(),
                    session.getCurrentQuestionIndex(),
                    session.getLanguage()
            );
        }

        return toResponse(session, currentQuestion);
    }

    @Transactional
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

    private SessionResponse toResponse(AssessmentSession session, QuestionDTO currentQuestion) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .currentLevel(session.getCurrentLevel())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .status(session.getStatus().name())
                .language(session.getLanguage())
                .startedAt(session.getStartedAt())
                .currentQuestion(currentQuestion)
                .build();
    }
}
