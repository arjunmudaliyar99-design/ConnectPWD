package org.connectpwd.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAnswerRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @NotBlank(message = "Question code is required")
    private String questionCode;
}
