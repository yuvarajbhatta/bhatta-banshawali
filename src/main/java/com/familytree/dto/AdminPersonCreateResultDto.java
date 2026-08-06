package com.familytree.dto;

import java.util.List;

/**
 * POST /api/v1/admin/persons's response -- the newly created person plus
 * a non-blocking, advisory list of existing records with a matching name
 * (DuplicateCandidateService#findLikelyDuplicatesOf), so the admin UI can
 * flag "this might already exist" right after creation instead of only in
 * the separate /admin/duplicates report. possibleDuplicates is empty, not
 * null, when nothing matched.
 */
public record AdminPersonCreateResultDto(
        AdminPersonDto person,
        List<AdminPersonDto> possibleDuplicates
) {
}
