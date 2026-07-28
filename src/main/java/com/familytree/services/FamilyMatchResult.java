package com.familytree.services;

import com.familytree.entity.MatchConfidence;

import java.util.List;

public record FamilyMatchResult(
        MatchConfidence confidence,
        List<CandidateEvaluation> candidateEvaluations
) {
    public List<Long> candidatePersonIds() {
        return candidateEvaluations.stream().map(e -> e.person().getId()).toList();
    }
}
