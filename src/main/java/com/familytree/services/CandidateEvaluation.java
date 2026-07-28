package com.familytree.services;

import com.familytree.entity.Person;

/**
 * How well one existing Person record (whose name matched the applicant's
 * stated name) fits the applicant's stated father/grandfather and DOB.
 */
public record CandidateEvaluation(
        Person person,
        boolean fatherNameMatches,
        boolean grandfatherNameMatches,
        boolean dobConflict
) {
    boolean isFullLineageMatch() {
        return fatherNameMatches && grandfatherNameMatches && !dobConflict;
    }

    boolean isPartialMatchWithoutConflict() {
        return fatherNameMatches && !dobConflict;
    }
}
