package com.familytree.dto;

/**
 * Response for GET /api/v1/me -- the logged-in member's own record and
 * immediate family, for the member dashboard (docs/08 Phase 4). "linked"
 * is false for an authenticated UserAccount that has no VERIFIED
 * UserPersonLink yet (approved but not yet matched to a Person by an
 * admin) -- person/family are null in that case, not an error, since the
 * account itself is perfectly valid.
 */
public record MemberProfileDto(
        String email,
        boolean linked,
        PersonSummaryDto person,
        FamilySnapshotDto family
) {
    public static MemberProfileDto unlinked(String email) {
        return new MemberProfileDto(email, false, null, null);
    }
}
