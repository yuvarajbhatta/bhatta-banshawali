package com.familytree.dto;

/**
 * A person with fewer than 2 recorded parents (docs/08 Phase 6 data-quality
 * report). generationNumber is included so the admin can visually discount
 * generation-1 people, who legitimately have 0 recorded parents -- this
 * report deliberately does not try to auto-detect "this is the root" itself,
 * since that's exactly the fragile heuristic RelationshipService#getRootPersonForLineage
 * already gets wrong (see docs/07-migration-plan.md).
 */
public record ParentGapDto(
        Long personId,
        String personName,
        Integer generationNumber,
        int knownParentCount
) {
}
