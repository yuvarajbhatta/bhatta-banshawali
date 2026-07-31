package com.familytree.dto;

import java.util.List;

/**
 * Bundled admin data-quality report (docs/08 Phase 6): missing parents,
 * relationship cycles, unlinked user accounts, and date issues -- one
 * fetch, one page, no live nav badge count (see DataQualityService).
 */
public record DataQualityReportDto(
        List<ParentGapDto> parentGaps,
        List<RelationshipCycleDto> cycles,
        List<AdminUserAccountDto> unlinkedAccounts,
        List<DateIssueDto> dateIssues
) {
}
