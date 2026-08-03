package com.familytree.dto;

import java.time.LocalDate;

/**
 * Full-fidelity Person view for the admin CRUD UI (docs/08 Phase 6) --
 * unlike PersonSummaryDto/PersonDetailDto, no per-viewer redaction:
 * every field visible here is already admin-only
 * ("/api/v1/admin/**" -> hasRole("ADMIN") in SecurityConfig).
 */
public record AdminPersonDto(
        Long id,
        Integer generationNumber,
        String firstName,
        String firstNameNepali,
        String middleName,
        String middleNameNepali,
        String lastName,
        String lastNameNepali,
        String nickname,
        String gender,
        LocalDate birthDate,
        LocalDate deathDate,
        String photoPath,
        String birthPlace,
        String currentAddress,
        String gotra,
        String notes
) {
}
