package org.connectpwd.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.connectpwd.question.dto.QuestionDTO;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private UUID sessionId;
    private int currentLevel;
    private int currentQuestionIndex;
    private String status;
    private String language;
    private Instant startedAt;
    private QuestionDTO currentQuestion;
}
