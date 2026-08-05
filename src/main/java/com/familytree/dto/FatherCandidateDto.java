package com.familytree.dto;

import java.util.List;

/**
 * A father candidate on the signup review screen, extending
 * MatchCandidateDto's shape with an optional corroborated mother match --
 * see FamilyMatchService.findSpouseMatchingName. matchedMother is null
 * unless this father already has a recorded spouse whose name matches
 * the applicant's submitted mother's name; when present, approving with
 * this candidate selected also links her as MOTHER (see
 * VerificationReviewService.approve's linkMatchedMother param).
 */
public record FatherCandidateDto(
        PersonSummaryDto person,
        List<PersonSummaryDto> ancestorChain,
        PersonSummaryDto matchedMother
) {
}
