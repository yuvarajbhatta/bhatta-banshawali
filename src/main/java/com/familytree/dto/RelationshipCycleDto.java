package com.familytree.dto;

import java.util.List;

/**
 * One ancestry cycle found by DataQualityService#findCycles -- personIds and
 * personNames are the same length and order, forming the chain
 * personIds.get(0) -> personIds.get(1) -> ... -> back to personIds.get(0).
 */
public record RelationshipCycleDto(
        List<Long> personIds,
        List<String> personNames
) {
}
