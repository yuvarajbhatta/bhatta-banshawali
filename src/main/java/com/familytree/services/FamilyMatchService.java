package com.familytree.services;

import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.repository.PersonRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Confidence-scored family-match engine for signup verification -- see
 * docs/05-auth-and-verification.md.
 *
 *   HIGH:   exactly one candidate whose name, father's name, and
 *           grandfather's name all match, with no DOB conflict.
 *   MEDIUM: more than one such candidate (ambiguous branch), or a
 *           candidate whose name and father's name match but the
 *           lineage chain is otherwise incomplete/unconfirmed.
 *   LOW:    no candidate's name matches at all, or every match that
 *           exists conflicts with the stated lineage or DOB.
 *
 * Never reveals which Person records were considered to the caller
 * beyond confidence + candidate IDs -- callers must not return this to
 * the applicant (see SignupService), only to admin review tooling.
 */
@Service
public class FamilyMatchService {

    private final PersonRepository personRepository;
    private final RelationshipService relationshipService;
    private final NameMatcher nameMatcher;
    private final PersonDisplayHelper personDisplayHelper;

    public FamilyMatchService(PersonRepository personRepository,
                              RelationshipService relationshipService,
                              NameMatcher nameMatcher,
                              PersonDisplayHelper personDisplayHelper) {
        this.personRepository = personRepository;
        this.relationshipService = relationshipService;
        this.nameMatcher = nameMatcher;
        this.personDisplayHelper = personDisplayHelper;
    }

    public FamilyMatchResult evaluateMatch(FamilyMatchRequest request) {
        List<Person> nameCandidates = personRepository.findAll().stream()
                .filter(person -> personNameMatches(person, request.applicantFullName()))
                .toList();

        if (nameCandidates.isEmpty()) {
            return new FamilyMatchResult(MatchConfidence.LOW, List.of());
        }

        List<CandidateEvaluation> evaluations = new ArrayList<>();
        for (Person candidate : nameCandidates) {
            evaluations.add(evaluateCandidate(candidate, request));
        }

        return new FamilyMatchResult(determineConfidence(evaluations), evaluations);
    }

    private CandidateEvaluation evaluateCandidate(Person candidate, FamilyMatchRequest request) {
        List<Person> parents = relationshipService.getParentsForPerson(candidate);

        Person matchingFather = parents.stream()
                .filter(parent -> personNameMatches(parent, request.fatherName()))
                .findFirst()
                .orElse(null);

        boolean grandfatherMatches = false;
        if (matchingFather != null) {
            List<Person> grandparents = relationshipService.getParentsForPerson(matchingFather);
            grandfatherMatches = grandparents.stream()
                    .anyMatch(grandparent -> personNameMatches(grandparent, request.grandfatherName()));
        }

        boolean dobConflict = request.dobAd() != null
                && candidate.getBirthDate() != null
                && !request.dobAd().equals(candidate.getBirthDate());

        return new CandidateEvaluation(candidate, matchingFather != null, grandfatherMatches, dobConflict);
    }

    private MatchConfidence determineConfidence(List<CandidateEvaluation> evaluations) {
        long fullLineageMatches = evaluations.stream().filter(CandidateEvaluation::isFullLineageMatch).count();

        if (fullLineageMatches == 1) {
            return MatchConfidence.HIGH;
        }
        if (fullLineageMatches > 1) {
            return MatchConfidence.MEDIUM;
        }
        boolean anyPartialMatch = evaluations.stream().anyMatch(CandidateEvaluation::isPartialMatchWithoutConflict);
        if (anyPartialMatch) {
            return MatchConfidence.MEDIUM;
        }
        return MatchConfidence.LOW;
    }

    private boolean personNameMatches(Person person, String submittedName) {
        return nameMatcher.namesMatch(personDisplayHelper.englishFullName(person), submittedName)
                || nameMatcher.namesMatch(personDisplayHelper.nepaliFullName(person), submittedName);
    }
}
