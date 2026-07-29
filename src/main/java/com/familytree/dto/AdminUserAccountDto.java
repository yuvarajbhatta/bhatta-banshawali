package com.familytree.dto;

import com.familytree.entity.UserAccountStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row in the admin "Manage User Accounts" page (docs/08 Phase 6) --
 * every UserAccount regardless of status or link state, combining what
 * used to be split across AdminAccountLinkApiController (link, unlinked
 * accounts only) and this controller (disable/enable, all accounts) so
 * an admin has one place to link, unlink, edit the applicant's
 * submitted info, disable/enable, or delete an account.
 */
public record AdminUserAccountDto(
        Long id,
        String email,
        UserAccountStatus status,
        String preferredLanguage,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        boolean isAdmin,
        Long linkedPersonId,
        String linkedPersonName,
        String submittedFullName,
        String submittedFatherName,
        String submittedMotherName,
        String submittedGrandfatherName,
        LocalDate submittedDobAd
) {
}
