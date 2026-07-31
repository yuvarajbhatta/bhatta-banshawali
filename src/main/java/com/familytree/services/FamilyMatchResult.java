package com.familytree.services;

import com.familytree.entity.MatchConfidence;

import java.util.List;

public record FamilyMatchResult(
        MatchConfidence confidence,
        List<CandidateEvaluation> existingPersonCandidates,
        List<NewPersonCandidateEvaluation> newPersonCandidates
) {
    public List<Long> existingPersonCandidateIds() {
        return existingPersonCandidates.stream().map(e -> e.person().getId()).toList();
    }

    public List<Long> newPersonFatherCandidateIds() {
        return newPersonCandidates.stream().map(e -> e.father().getId()).toList();
    }
}
