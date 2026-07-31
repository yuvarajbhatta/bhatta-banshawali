package com.familytree.services;

import com.familytree.entity.Person;

/**
 * Evidence that an existing Person (father) matched the applicant's stated
 * father's name, and whether that father's own recorded parent corroborates
 * the applicant's stated grandfather's name -- the basis for offering
 * "create a new Person as a child of this father" on the admin review
 * screen, for applicants who don't yet exist in the tree but whose father
 * does. There is no DOB-conflict concept here -- there is no existing
 * applicant Person to compare a birth date against.
 */
public record NewPersonCandidateEvaluation(
        Person father,
        boolean grandfatherNameMatches,
        boolean anyFuzzyMatch
) {
    boolean isFullLineageMatch() {
        return grandfatherNameMatches && !anyFuzzyMatch;
    }
}
