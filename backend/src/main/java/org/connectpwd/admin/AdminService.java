package org.connectpwd.admin;

import lombok.RequiredArgsConstructor;
import org.connectpwd.admin.dto.*;
import org.connectpwd.answer.ResponseDocument;
import org.connectpwd.answer.ResponseRepository;
import org.connectpwd.report.ReportRepository;
import org.connectpwd.scoring.IsaaScore;
import org.connectpwd.scoring.IsaaScoreRepository;
import org.connectpwd.session.AssessmentSession;
import org.connectpwd.session.SessionRepository;
import org.connectpwd.user.User;
import org.connectpwd.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ResponseRepository responseRepository;
    private final IsaaScoreRepository isaaScoreRepository;
    private final ReportRepository reportRepository;

    // ---- Users -------------------------------------------------------

    public Page<AdminUserDTO> listUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);

        List<String> userIds = page.stream().map(User::getId).toList();
        Map<String, Long> countMap = sessionRepository.countByUserIdIn(userIds);

        List<AdminUserDTO> dtos = page.stream()
                .map(u -> toUserDTO(u, countMap.getOrDefault(u.getId(), 0L)))
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // ---- Sessions ----------------------------------------------------

    public Page<AdminSessionDTO> listSessions(Pageable pageable) {
        Page<AssessmentSession> page = sessionRepository.findAll(pageable);

        List<String> userIds = page.stream().map(AssessmentSession::getUserId).distinct().toList();
        Map<String, User> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<AdminSessionDTO> dtos = page.stream()
                .map(s -> toSessionDTO(s, userMap.get(s.getUserId())))
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // ---- Session detail (MongoDB responses) --------------------------

    public List<AdminResponseDTO> listResponses(String sessionId) {
        return responseRepository.findBySessionId(sessionId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ---- ISAA Score for a session ------------------------------------

    public IsaaScore getScore(String sessionId) {
        return isaaScoreRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Score not found"));
    }

    // ---- Platform stats ----------------------------------------------

    public AdminStatsDTO stats() {
        long totalUsers        = userRepository.count();
        long activeSessions    = sessionRepository.countByStatus(org.connectpwd.session.SessionStatus.IN_PROGRESS);
        long completedSessions = sessionRepository.countByStatus(org.connectpwd.session.SessionStatus.COMPLETED);
        long totalReports      = reportRepository.count();
        return new AdminStatsDTO(totalUsers, activeSessions, completedSessions, totalReports);
    }

    // ---- Mappers (no passwordHash ever exposed) ----------------------

    private AdminUserDTO toUserDTO(User u, long sessionCount) {
        return new AdminUserDTO(
                u.getId(), u.getFullName(), u.getEmail(),
                u.getRole(), u.getPhone(), u.getLanguage(),
                u.isActive(), u.getCreatedAt(), sessionCount
        );
    }

    private AdminSessionDTO toSessionDTO(AssessmentSession s, User u) {
        String email    = u != null ? u.getEmail()    : null;
        String fullName = u != null ? u.getFullName() : null;
        return new AdminSessionDTO(
                s.getId(), s.getUserId(), email, fullName,
                s.getModuleType(), s.getTriageSeekingFor(), s.getTriageAge(),
                s.getStatus().name(), s.getLanguage(),
                s.getCurrentLevel(), s.getStartedAt(), s.getCompletedAt()
        );
    }

    private AdminResponseDTO toResponseDTO(ResponseDocument r) {
        return new AdminResponseDTO(
                r.getId(), r.getQuestionCode(), r.getQuestionText(),
                r.getDomain(), r.getLevel(), r.getAnswerType(),
                r.getAnswerText(), r.getScaleValue(), r.getAudioKey(),
                r.getTranscript(), r.getAnsweredAt()
        );
    }
}
