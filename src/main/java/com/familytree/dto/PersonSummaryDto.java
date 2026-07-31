package com.familytree.dto;

import java.time.LocalDate;

/**
 * A trimmed-down Person for member-facing API responses (docs/05, member
 * dashboard) -- deliberately excludes internal-only fields like notes,
 * currentAddress, and photoPath that the admin tooling shows but a
 * profile/family-snapshot view has no need for.
 *
 * parentHint is null for every use except direct search results
 * (PersonProfileAssembler.summarizeForSearch) -- it exists solely so a
 * picker choosing between several same-named people (e.g. multiple
 * "Bhojraj Bhatta") can tell them apart by father's name, without
 * computing it recursively for every father/mother/spouse/child entry
 * a family snapshot already returns.
 */
public record PersonSummaryDto(
        Long id,
        String englishFullName,
        String nepaliFullName,
        Integer generationNumber,
        String gender,
        LocalDate birthDate,
        String parentHint
) {
}
