package org.connectpwd.admin.dto;

import java.time.Instant;

public record AdminSessionDTO(
        String id,
        String userId,
        String userEmail,
        String userFullName,
        String moduleType,
        String triageSeekingFor,
        Integer triageAge,
        String status,
        String language,
        int currentLevel,
        Instant startedAt,
        Instant completedAt
) {}
