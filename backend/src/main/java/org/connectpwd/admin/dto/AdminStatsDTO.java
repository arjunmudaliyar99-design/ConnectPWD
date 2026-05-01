package org.connectpwd.admin.dto;

public record AdminStatsDTO(
        long totalUsers,
        long activeSessions,
        long completedSessions,
        long totalReports
) {}
