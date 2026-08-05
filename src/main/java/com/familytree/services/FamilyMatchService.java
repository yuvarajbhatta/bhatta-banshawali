package com.familytree.services;

import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.repository.PersonRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Confidence-scored family-match engine for signup verification -- see
 * docs/05-auth-and-verification.md.
 *
 * Runs two independent, additive search strategies and merges their results
 * into one confidence score:
 *
 *   Strategy A (existing person): search for a Person whose name matches
 *   the APPLICANT'S OWN stated name, then corroborate via father/
 *   grandfather. Only ever finds something if the applicant already exists
 *   as a pre-populated placeholder Person -- useful for pre-built trees,
 *   but produces nothing for the much more common case of a brand-new
 *   family member who was never entered into the tree before.
 *
 *   Strategy B (new person, existing father): search for a Person whose
 *   name matches the applicant's stated FATHER's name, then corroborate via
 *   that father's own recorded father against the stated grandfather's
 *   name. This is what actually connects a brand-new applicant into the
 *   tree when their father/grandfather are already properly recorded --
 *   the previous fully applicant-name-keyed approach could never find this.
 *   A father having many other children already linked into the tree is
 *   normal family structure, never treated as a conflict or exclusion --
 *   the one exception is a father Strategy A already matched to an
 *   existing Person representing THIS SAME applicant, which Strategy B
 *   skips entirely (see evaluateNewPersonStrategy), since offering to also
 *   create a duplicate there would just double-count the same real person.
 *
 *   HIGH:   exactly one candidate (from either strategy) has a full,
 *           exact-or-transliteration lineage match with no DOB conflict.
 *   MEDIUM: more than one such candidate (ambiguous, even across
 *           strategies -- there are genuinely two different plausible
 *           actions to choose between), or any candidate with a partial/
 *           fuzzy signal (father-name match without a confirmed
 *           grandfather, or any hop relying on a fuzzy spelling-variant
 *           match rather than exact).
 *   LOW:    neither strategy found anything at all.
 *
 * Never reveals which Person records were considered to the caller beyond
 * confidence + candidate IDs -- callers must not return this to the
 * applicant (see SignupService), only to admin review tooling.
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
        Set<Long> fathersAlreadyMatchedToAnExistingPerson = new HashSet<>();
        List<CandidateEvaluation> existingPersonCandidates =
                evaluateExistingPersonStrategy(request, fathersAlreadyMatchedToAnExistingPerson);
        List<NewPersonCandidateEvaluation> newPersonCandidates =
                evaluateNewPersonStrategy(request, fathersAlreadyMatchedToAnExistingPerson);
        MatchConfidence confidence = determineConfidence(existingPersonCandidates, newPersonCandidates);
        return new FamilyMatchResult(confidence, existingPersonCandidates, newPersonCandidates);
    }

    private List<CandidateEvaluation> evaluateExistingPersonStrategy(FamilyMatchRequest request,
                                                                      Set<Long> fathersAlreadyMatchedToAnExistingPerson) {
        List<Person> nameCandidates = personRepository.findAll().stream()
                .filter(person -> personNameMatchQuality(person, request.applicantFullName()) != NameMatchQuality.NONE)
                .toList();
        return nameCandidates.stream()
                .map(candidate -> evaluateExistingPersonCandidate(candidate, request, fathersAlreadyMatchedToAnExistingPerson))
                .toList();
    }

    private CandidateEvaluation evaluateExistingPersonCandidate(Person candidate, FamilyMatchRequest request,
                                                                 Set<Long> fathersAlreadyMatchedToAnExistingPerson) {
        NameMatchQuality nameQuality = personNameMatchQuality(candidate, request.applicantFullName());

        List<Person> parents = relationshipService.getParentsForPerson(candidate);
        ParentMatch fatherMatch = findBestMatchingParent(parents, request.fatherName());

        boolean grandfatherMatches = false;
        NameMatchQuality grandfatherQuality = NameMatchQuality.NONE;
        if (fatherMatch != null) {
            // This father already has a matching-named child on record (this
            // candidate) -- Strategy B must not also offer to create a new,
            // duplicate child under the same father for the same applicant.
            fathersAlreadyMatchedToAnExistingPerson.add(fatherMatch.person().getId());

            List<Person> grandparents = relationshipService.getParentsForPerson(fatherMatch.person());
            ParentMatch grandfatherMatch = findBestMatchingParent(grandparents, request.grandfatherName());
            grandfatherMatches = grandfatherMatch != null;
            grandfatherQuality = grandfatherMatch != null ? grandfatherMatch.quality() : NameMatchQuality.NONE;
        }

        boolean dobConflict = request.dobAd() != null
                && candidate.getBirthDate() != null
                && !request.dobAd().equals(candidate.getBirthDate());

        boolean anyFuzzy = nameQuality == NameMatchQuality.FUZZY
                || (fatherMatch != null && fatherMatch.quality() == NameMatchQuality.FUZZY)
                || grandfatherQuality == NameMatchQuality.FUZZY;

        return new CandidateEvaluation(candidate, fatherMatch != null, grandfatherMatches, dobConflict, anyFuzzy);
    }

    // Explicitly does NOT exclude or penalize a father candidate for
    // already having OTHER children linked in the tree -- that's normal
    // family structure, not a duplicate (see class javadoc). The one
    // exclusion that IS applied (fathersAlreadyMatchedToAnExistingPerson)
    // is narrower and different: it only skips a father when Strategy A
    // already found an existing Person matching THIS SAME applicant under
    // THIS SAME father, since offering to also create a new one there
    // would just be a duplicate of what Strategy A already covers.
    private List<NewPersonCandidateEvaluation> evaluateNewPersonStrategy(FamilyMatchRequest request,
                                                                          Set<Long> fathersAlreadyMatchedToAnExistingPerson) {
        List<ParentMatch> fatherCandidates = personRepository.findAll().stream()
                .map(person -> new ParentMatch(person, personNameMatchQuality(person, request.fatherName())))
                .filter(match -> match.quality() != NameMatchQuality.NONE)
                .filter(match -> !fathersAlreadyMatchedToAnExistingPerson.contains(match.person().getId()))
                .toList();

        List<NewPersonCandidateEvaluation> evaluations = new ArrayList<>();
        for (ParentMatch fatherMatch : fatherCandidates) {
            List<Person> grandparents = relationshipService.getParentsForPerson(fatherMatch.person());
            ParentMatch grandfatherMatch = findBestMatchingParent(grandparents, request.grandfatherName());

            boolean anyFuzzy = fatherMatch.quality() == NameMatchQuality.FUZZY
                    || (grandfatherMatch != null && grandfatherMatch.quality() == NameMatchQuality.FUZZY);

            evaluations.add(new NewPersonCandidateEvaluation(fatherMatch.person(), grandfatherMatch != null, anyFuzzy));
        }
        return evaluations;
    }

    private MatchConfidence determineConfidence(List<CandidateEvaluation> existingPersonCandidates,
                                                 List<NewPersonCandidateEvaluation> newPersonCandidates) {
        long fullLineageMatches = existingPersonCandidates.stream().filter(CandidateEvaluation::isFullLineageMatch).count()
                + newPersonCandidates.stream().filter(NewPersonCandidateEvaluation::isFullLineageMatch).count();

        if (fullLineageMatches == 1) {
            return MatchConfidence.HIGH;
        }
        if (fullLineageMatches > 1) {
            return MatchConfidence.MEDIUM;
        }

        boolean anyPartialMatch = existingPersonCandidates.stream().anyMatch(CandidateEvaluation::isPartialMatchWithoutConflict)
                || !newPersonCandidates.isEmpty();
        return anyPartialMatch ? MatchConfidence.MEDIUM : MatchConfidence.LOW;
    }

    /**
     * Looks for a recorded spouse of {@code person} whose name matches
     * {@code submittedName} -- used by the signup-review "create as child
     * of this father" flow to corroborate the applicant's submitted
     * mother's name against that father's already-linked spouse, rather
     * than running an independent, uncorroborated search over every
     * Person for the mother's name (see FamilyMatchService's class
     * javadoc reasoning for why grandfather-style corroboration keeps the
     * father search from being noisy -- a mother-name-only search has no
     * equivalent second hop to corroborate against, so this piggybacks on
     * the father match instead). Handles multiple recorded spouses (e.g.
     * remarriage) for free via the same EXACT-preferred/FUZZY-fallback
     * logic {@link #findBestMatchingParent} already uses for parents.
     */
    public Optional<Person> findSpouseMatchingName(Person person, String submittedName) {
        if (submittedName == null || submittedName.isBlank()) {
            return Optional.empty();
        }
        ParentMatch match = findBestMatchingParent(relationshipService.getSpousesForPerson(person), submittedName);
        return Optional.ofNullable(match).map(ParentMatch::person);
    }

    private record ParentMatch(Person person, NameMatchQuality quality) {
    }

    private NameMatchQuality personNameMatchQuality(Person person, String submittedName) {
        NameMatchQuality english = nameMatcher.matchQuality(personDisplayHelper.englishFullName(person), submittedName);
        if (english == NameMatchQuality.EXACT) {
            return NameMatchQuality.EXACT;
        }
        NameMatchQuality nepali = nameMatcher.matchQuality(personDisplayHelper.nepaliFullName(person), submittedName);
        if (nepali == NameMatchQuality.EXACT) {
            return NameMatchQuality.EXACT;
        }
        return (english == NameMatchQuality.FUZZY || nepali == NameMatchQuality.FUZZY)
                ? NameMatchQuality.FUZZY : NameMatchQuality.NONE;
    }

    /** Prefers an EXACT match over a FUZZY one; returns null if nothing matched at all. */
    private ParentMatch findBestMatchingParent(List<Person> parents, String submittedName) {
        ParentMatch best = null;
        for (Person parent : parents) {
            NameMatchQuality quality = personNameMatchQuality(parent, submittedName);
            if (quality == NameMatchQuality.EXACT) {
                return new ParentMatch(parent, quality);
            }
            if (quality == NameMatchQuality.FUZZY && best == null) {
                best = new ParentMatch(parent, quality);
            }
        }
        return best;
    }
}
