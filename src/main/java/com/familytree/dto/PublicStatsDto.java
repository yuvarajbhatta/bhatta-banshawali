package com.familytree.dto;

/**
 * Privacy-safe aggregate figures for the public landing page -- see
 * docs/06-ui-ux-specification.md "Public Statistics". Deliberately
 * contains no names, no individual records, nothing identifying.
 */
public record PublicStatsDto(
        long documentedFamilyMembers,
        long documentedGenerations,
        Integer oldestDocumentedGeneration
) {
}
