package org.connectpwd.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextAnswerRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @NotBlank(message = "Question code is required")
    private String questionCode;

    @NotBlank(message = "Answer type is required")
    @Pattern(regexp = "^(SCALE|CHOICE|TEXT)$", message = "Answer type must be SCALE, CHOICE, or TEXT")
    private String answerType;

    private String answerText;

    @Range(min = 1, max = 5, message = "Scale value must be between 1 and 5")
    private Integer scaleValue;
}
