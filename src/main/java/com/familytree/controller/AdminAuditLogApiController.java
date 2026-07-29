package com.familytree.controller;

import com.familytree.dto.AuditLogEntryDto;
import com.familytree.entity.AuditLogEntry;
import com.familytree.services.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only view of admin actions (docs/08 Phase 6), recorded by
 * AuditLogService from the service layer -- covers both the legacy
 * Thymeleaf admin pages and the newer REST admin UI. Admin-only via
 * "/api/v1/admin/**" in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-log")
public class AdminAuditLogApiController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    private final AuditLogService auditLogService;

    public AdminAuditLogApiController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLogEntryDto> recent(@RequestParam(required = false) Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        return auditLogService.recent(resolvedLimit).stream().map(this::toDto).toList();
    }

    private AuditLogEntryDto toDto(AuditLogEntry entry) {
        return new AuditLogEntryDto(
                entry.getId(),
                entry.getActorUsername(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getSummary(),
                entry.getCreatedAt()
        );
    }
}
