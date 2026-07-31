package com.familytree.dto;

/**
 * issueType is a plain string constant (see DataQualityService), not an
 * enum -- matches AuditLogService's existing "free string, no migration
 * needed for a new type" convention for action/entityType.
 */
public record DateIssueDto(
        Long personId,
        String personName,
        String issueType,
        String detail
) {
}
