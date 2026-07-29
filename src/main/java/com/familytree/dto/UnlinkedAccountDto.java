package com.familytree.dto;

import java.time.LocalDateTime;

/**
 * An approved (ACTIVE) account with no VERIFIED UserPersonLink yet --
 * see VerificationReviewService#findUnlinkedActiveAccounts. The
 * submitted* fields are the applicant's original signup answers (from
 * their VerificationRequest, if one still exists), shown so the admin
 * doesn't have to guess who this account belongs to.
 */
public record UnlinkedAccountDto(
        Long userAccountId,
        String email,
        LocalDateTime createdAt,
        String submittedFullName,
        String submittedFatherName,
        String submittedGrandfatherName
) {
}
