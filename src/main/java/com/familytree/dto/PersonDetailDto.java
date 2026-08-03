package com.familytree.dto;

import java.time.LocalDate;

/**
 * Full member-facing Person view for GET /api/v1/persons/{id} (docs/08
 * Phase 4 person detail pages) -- a superset of PersonSummaryDto used
 * for the family-snapshot lists, matching what person-details.html
 * already shows an authenticated member today.
 */
public record PersonDetailDto(
        Long id,
        String englishFullName,
        String nepaliFullName,
        String nickname,
        String gender,
        Integer generationNumber,
        LocalDate birthDate,
        LocalDate deathDate,
        String birthPlace,
        String currentAddress,
        String gotra,
        String notes,
        String photoPath,
        FamilySnapshotDto family
) {
}
