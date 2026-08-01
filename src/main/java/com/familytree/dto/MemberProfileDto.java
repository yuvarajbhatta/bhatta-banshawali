package com.familytree.dto;

import java.util.List;

/**
 * Response for GET /api/v1/me -- the logged-in member's own record and
 * immediate family, for the member dashboard (docs/08 Phase 4). "linked"
 * is false for an authenticated UserAccount that has no VERIFIED
 * UserPersonLink yet (approved but not yet matched to a Person by an
 * admin) -- person/family are null in that case, not an error, since the
 * account itself is perfectly valid.
 *
 * ancestorChain (self included, as far back as the FATHER line goes --
 * see PersonProfileAssembler.ancestorChain) is the same lineage view an
 * admin sees when linking a signup, reused here so members can see it
 * on their own dashboard.
 */
public record MemberProfileDto(
        String email,
        boolean linked,
        PersonSummaryDto person,
        FamilySnapshotDto family,
        List<PersonSummaryDto> ancestorChain
) {
    public static MemberProfileDto unlinked(String email) {
        return new MemberProfileDto(email, false, null, null, List.of());
    }
}
