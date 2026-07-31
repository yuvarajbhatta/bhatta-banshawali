package com.familytree.services;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encodes/decodes the comma-separated Person-ID string columns on
 * VerificationRequest (matchedCandidatePersonIds, matchedFatherCandidatePersonIds)
 * -- plain string columns, not join tables, by existing convention.
 */
public final class CommaSeparatedIds {

    private CommaSeparatedIds() {
    }

    public static String join(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static List<Long> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
