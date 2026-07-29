package com.familytree.dto;

import com.familytree.entity.MatchConfidence;
import com.familytree.entity.VerificationStatus;

import java.time.LocalDateTime;

/**
 * One row in the admin signup review queue (docs/05, docs/06
 * "Admin -- Signup Review"). Mirrors what admin-signups.html already
 * shows -- this is a REST replacement for that Thymeleaf list, not new
 * functionality.
 */
public record AdminSignupSummaryDto(
        Long id,
        String submittedFullName,
        String submittedFatherName,
        String submittedGrandfatherName,
        MatchConfidence matchConfidence,
        VerificationStatus status,
        LocalDateTime createdAt
) {
}
