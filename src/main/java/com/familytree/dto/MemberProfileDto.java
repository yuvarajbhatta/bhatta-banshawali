package com.familytree.dto;

import java.time.LocalDate;

/**
 * Response for GET /api/v1/me -- the logged-in member's own record and
 * immediate family, for the member dashboard (docs/08 Phase 4). "linked"
 * is false for an authenticated UserAccount that has no VERIFIED
 * UserPersonLink yet (approved but not yet matched to a Person by an
 * admin) -- person/family are null in that case, not an error, since the
 * account itself is perfectly valid.
 *
 * memberSince and pendingCorrectionCount describe the UserAccount
 * itself, so both are populated whether or not the account is linked
 * yet; gotra comes from the linked Person and is null until then.
 */
public record MemberProfileDto(
        String email,
        boolean linked,
        PersonSummaryDto person,
        FamilySnapshotDto family,
        String gotra,
        LocalDate memberSince,
        long pendingCorrectionCount
) {
    public static MemberProfileDto unlinked(String email, LocalDate memberSince, long pendingCorrectionCount) {
        return new MemberProfileDto(email, false, null, null, null, memberSince, pendingCorrectionCount);
    }
}
