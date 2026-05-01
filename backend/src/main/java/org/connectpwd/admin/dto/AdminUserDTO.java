package org.connectpwd.admin.dto;

import org.connectpwd.user.UserRole;

import java.time.Instant;

public record AdminUserDTO(
        String id,
        String fullName,
        String email,
        UserRole role,
        String phone,
        String language,
        boolean isActive,
        Instant createdAt,
        long sessionCount
) {}
