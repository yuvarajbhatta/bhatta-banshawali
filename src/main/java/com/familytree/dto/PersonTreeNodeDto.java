package com.familytree.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One person as a node in the whole-family graph (docs/08 Phase 5,
 * GET /api/v1/family-tree). Flat + id-referencing rather than nested,
 * because a strict parent-to-children tree can't represent a spouse
 * co-parenting the same children without duplicating nodes -- the
 * client-side layout (Dagre) builds the visual hierarchy from these
 * references instead.
 *
 * birthDate follows the same per-viewer redaction rule as
 * PersonSummaryDto/PersonDetailDto (PersonProfileAssembler) -- null
 * unless the viewer is an admin or this is the viewer's own linked
 * person.
 */
public record PersonTreeNodeDto(
        Long id,
        String englishFullName,
        String nepaliFullName,
        String gender,
        Integer generationNumber,
        LocalDate birthDate,
        LocalDate deathDate,
        Long fatherId,
        Long motherId,
        List<Long> spouseIds,
        List<Long> childIds
) {
}
