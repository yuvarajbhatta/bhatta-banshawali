package com.familytree.dto;

import com.familytree.entity.MatchConfidence;
import com.familytree.entity.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full submitted profile + match evidence for one signup request
 * (docs/06 "Match Evidence panel"), mirrors admin-signup-detail.html.
 * Neither candidates nor fatherCandidates is ever shown to the applicant
 * themselves -- see VerificationRequest.matchedCandidatePersonIds /
 * matchedFatherCandidatePersonIds. candidates are existing Person records
 * to link the account to directly; fatherCandidates are existing fathers
 * to create a brand-new Person as a child of. Each candidate's
 * ancestorChain (see PersonProfileAssembler.ancestorChain) is what lets
 * the admin tell apart several same-named candidates by their actual
 * lineage, instead of an opaque ID.
 */
public record AdminSignupDetailDto(
        Long id,
        String submittedFullName,
        String submittedFullNameNepali,
        String submittedFatherName,
        String submittedGrandfatherName,
        LocalDate submittedDobAd,
        Integer submittedDobBsYear,
        Integer submittedDobBsMonth,
        Integer submittedDobBsDay,
        String motherName,
        String placeOfBirth,
        String ancestralVillage,
        String familyBranch,
        String knownRelativeName,
        String invitationCode,
        String applicantNote,
        MatchConfidence matchConfidence,
        VerificationStatus status,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String decisionNote,
        LocalDateTime createdAt,
        List<MatchCandidateDto> candidates,
        List<FatherCandidateDto> fatherCandidates
) {
}
