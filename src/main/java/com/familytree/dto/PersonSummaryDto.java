package com.familytree.dto;

import java.time.LocalDate;

/**
 * A trimmed-down Person for member-facing API responses (docs/05, member
 * dashboard) -- deliberately excludes internal-only fields like notes,
 * currentAddress, and photoPath that the admin tooling shows but a
 * profile/family-snapshot view has no need for.
 */
public record PersonSummaryDto(
        Long id,
        String englishFullName,
        String nepaliFullName,
        Integer generationNumber,
        String gender,
        LocalDate birthDate
) {
}
