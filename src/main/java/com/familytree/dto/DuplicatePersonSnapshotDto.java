package com.familytree.dto;

import java.time.LocalDate;

/**
 * One side of a DuplicateCandidateDto pair. populatedFieldCount is computed
 * server-side (count of non-blank fields) so the frontend can default the
 * "keep this one" survivor selection to whichever record has more data,
 * without reimplementing that logic in TypeScript.
 */
public record DuplicatePersonSnapshotDto(
        Long id,
        String englishFullName,
        String nepaliFullName,
        String gender,
        LocalDate birthDate,
        LocalDate deathDate,
        Integer generationNumber,
        int populatedFieldCount
) {
}
