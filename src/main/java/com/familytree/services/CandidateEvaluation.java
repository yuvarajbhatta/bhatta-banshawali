package com.familytree.services;

import com.familytree.entity.Person;

/**
 * How well one existing Person record (whose name matched the applicant's
 * stated name) fits the applicant's stated father/grandfather and DOB.
 * anyFuzzyMatch is true if any hop in the chain (applicant/father/
 * grandfather name) relied on a same-script spelling-variant match rather
 * than an exact/transliteration one -- a fuzzy hop can never by itself
 * produce a HIGH-confidence result (see isFullLineageMatch()), only MEDIUM.
 */
public record CandidateEvaluation(
        Person person,
        boolean fatherNameMatches,
        boolean grandfatherNameMatches,
        boolean dobConflict,
        boolean anyFuzzyMatch
) {
    boolean isFullLineageMatch() {
        return fatherNameMatches && grandfatherNameMatches && !dobConflict && !anyFuzzyMatch;
    }

    boolean isPartialMatchWithoutConflict() {
        return fatherNameMatches && !dobConflict;
    }
}
