package com.familytree.dto;

import com.familytree.entity.MatchConfidence;

import java.util.List;

/**
 * A candidate pair of Person records that may be the same person
 * (docs/08 Phase 6). reasons includes both corroborating and conflicting
 * signals -- conflicts are surfaced, not hidden, so the admin can quickly
 * dismiss a weak match. hasConflict is broken out separately so the
 * frontend can flag it without string-matching reasons.
 */
public record DuplicateCandidateDto(
        DuplicatePersonSnapshotDto personA,
        DuplicatePersonSnapshotDto personB,
        MatchConfidence confidence,
        List<String> reasons,
        boolean hasConflict
) {
}
