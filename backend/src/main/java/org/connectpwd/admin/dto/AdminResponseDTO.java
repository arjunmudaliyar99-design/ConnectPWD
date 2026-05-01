package org.connectpwd.admin.dto;

import java.time.Instant;

public record AdminResponseDTO(
        String id,
        String questionCode,
        String questionText,
        String domain,
        int level,
        String answerType,
        String answerText,
        Integer scaleValue,
        String audioKey,
        String transcript,
        Instant answeredAt
) {}
