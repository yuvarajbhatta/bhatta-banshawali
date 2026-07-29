package com.familytree.dto;

import java.time.LocalDateTime;

public record AuditLogEntryDto(
        Long id,
        String actorUsername,
        String action,
        String entityType,
        Long entityId,
        String summary,
        LocalDateTime createdAt
) {
}
