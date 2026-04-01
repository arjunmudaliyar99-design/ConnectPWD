package org.connectpwd.answer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.connectpwd.question.dto.QuestionDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private QuestionDTO nextQuestion;
    private boolean levelComplete;
    private Integer nextLevel;
    private String sessionStatus;
}
