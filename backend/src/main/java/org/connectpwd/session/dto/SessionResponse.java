package org.connectpwd.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.connectpwd.question.dto.QuestionDTO;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private String sessionId;
    private String moduleType;
    private int currentLevel;
    private int currentQuestionIndex;
    private int totalQuestions;
    private String status;
    private String language;
    private Instant startedAt;
    private QuestionDTO currentQuestion;
}
