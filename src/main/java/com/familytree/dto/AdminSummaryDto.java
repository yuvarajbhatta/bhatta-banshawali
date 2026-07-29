package com.familytree.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin control-center summary for the dashboard (docs/08 Phase 4 UI
 * overhaul) -- counts and a short recent list for each pending review
 * queue, so an admin sees what needs attention without leaving the
 * dashboard. Full review/decision actions still happen on the existing
 * /admin/signups and /admin/corrections pages; this is read-only.
 */
public record AdminSummaryDto(
        int pendingSignupCount,
        int pendingCorrectionCount,
        List<PendingSignupSummary> recentPendingSignups,
        List<PendingCorrectionSummary> recentPendingCorrections
) {
    public record PendingSignupSummary(Long id, String submittedFullName, LocalDateTime submittedAt) {
    }

    public record PendingCorrectionSummary(Long id, String personName, String field, LocalDateTime submittedAt) {
    }
}
