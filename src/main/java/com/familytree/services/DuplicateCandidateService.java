package com.familytree.services;

import com.familytree.dto.DuplicateCandidateDto;
import com.familytree.dto.DuplicatePersonSnapshotDto;
import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds candidate duplicate Person pairs for the guided merge tool
 * (docs/08 Phase 6) -- never merges automatically, only surfaces candidates
 * for an admin to review via PersonMergeService. Pairwise O(n^2) scan is
 * acceptable at family-tree scale and only runs when an admin opens this
 * report (never wired into nav badge counts -- see DataQualityService's
 * Javadoc for the same reasoning).
 *
 * Reuses NameMatcher#namesMatch (already tested for the signup family-match
 * engine) rather than hand-rolling a "faster" precomputed equality check --
 * simplicity and identical, already-tested matching semantics won over a
 * speculative optimization neither required for correctness nor asked for.
 */
@Service
public class DuplicateCandidateService {

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipService relationshipService;
    private final NameMatcher nameMatcher;
    private final PersonDisplayHelper personDisplay;

    public DuplicateCandidateService(PersonRepository personRepository,
                                     RelationshipRepository relationshipRepository,
                                     RelationshipService relationshipService,
                                     NameMatcher nameMatcher,
                                     PersonDisplayHelper personDisplay) {
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
        this.relationshipService = relationshipService;
        this.nameMatcher = nameMatcher;
        this.personDisplay = personDisplay;
    }

    /**
     * Save-time equivalent of {@link #findCandidates()} -- checks one
     * candidate Person against everyone already in the register, for a
     * non-blocking "this looks like it might already exist" warning at
     * creation/edit time. PersonService itself never blocked a same-named
     * save and this doesn't either -- family trees legitimately have
     * repeated names across cousins/generations (docs/09-security-threat-
     * model.md notes this was a known, accepted gap), so this stays purely
     * advisory, same as the full findCandidates() report.
     */
    public List<Person> findLikelyDuplicatesOf(Person candidate) {
        return personRepository.findAll().stream()
                .filter(existing -> candidate.getId() == null || !existing.getId().equals(candidate.getId()))
                .filter(existing -> namesMatch(candidate, existing))
                .toList();
    }

    public List<DuplicateCandidateDto> findCandidates() {
        List<Person> people = personRepository.findAll();
        Set<String> directlyRelatedPairs = buildDirectlyRelatedPairKeys();

        List<DuplicateCandidateDto> candidates = new ArrayList<>();
        for (int i = 0; i < people.size(); i++) {
            for (int j = i + 1; j < people.size(); j++) {
                Person a = people.get(i);
                Person b = people.get(j);

                // Directly-related pairs are excluded from candidate
                // generation entirely (not just refused at merge time) --
                // showing an admin an unmergeable pair is confusing UX, and
                // two directly-related people being flagged as duplicates
                // is almost certainly a candidate-generation bug, not a
                // real duplicate. PersonMergeService re-checks this too,
                // since this candidate list can go stale between when it's
                // fetched and when the admin clicks merge.
                if (directlyRelatedPairs.contains(pairKey(a.getId(), b.getId()))) {
                    continue;
                }
                if (!namesMatch(a, b)) {
                    continue;
                }

                candidates.add(scoreCandidate(a, b));
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparing(dto -> dto.confidence().ordinal()))
                .toList();
    }

    private Set<String> buildDirectlyRelatedPairKeys() {
        Set<String> keys = new HashSet<>();
        for (Relationship relationship : relationshipRepository.findAll()) {
            keys.add(pairKey(relationship.getPerson().getId(), relationship.getRelatedPerson().getId()));
        }
        return keys;
    }

    private String pairKey(Long idA, Long idB) {
        long min = Math.min(idA, idB);
        long max = Math.max(idA, idB);
        return min + ":" + max;
    }

    private DuplicateCandidateDto scoreCandidate(Person a, Person b) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Names match");

        boolean dobAgreement = a.getBirthDate() != null && b.getBirthDate() != null
                && a.getBirthDate().equals(b.getBirthDate());
        boolean dobConflict = a.getBirthDate() != null && b.getBirthDate() != null
                && !a.getBirthDate().equals(b.getBirthDate());
        if (dobAgreement) {
            reasons.add("Birth dates match (" + a.getBirthDate() + ")");
        } else if (dobConflict) {
            reasons.add("Conflicting birth dates: " + a.getBirthDate() + " vs " + b.getBirthDate());
        }

        List<Person> parentsA = relationshipService.getParentsForPerson(a);
        List<Person> parentsB = relationshipService.getParentsForPerson(b);
        Person matchingParent = findMatchingParent(parentsA, parentsB);
        boolean parentAgreement = matchingParent != null;
        // Only a real conflict signal when both sides actually have parent
        // data to compare -- one-sided missing data isn't a conflict.
        boolean parentConflict = !parentAgreement && !parentsA.isEmpty() && !parentsB.isEmpty();

        if (parentAgreement) {
            reasons.add("Shared parent: " + personDisplay.englishFullName(matchingParent));
        } else if (parentConflict) {
            reasons.add("Conflicting parent names: " + describeNames(parentsA) + " vs " + describeNames(parentsB));
        }

        boolean hasConflict = dobConflict || parentConflict;
        MatchConfidence confidence;
        if (hasConflict) {
            confidence = MatchConfidence.LOW;
        } else if (dobAgreement || parentAgreement) {
            confidence = MatchConfidence.HIGH;
        } else {
            confidence = MatchConfidence.MEDIUM;
        }

        return new DuplicateCandidateDto(toSnapshot(a), toSnapshot(b), confidence, reasons, hasConflict);
    }

    private Person findMatchingParent(List<Person> parentsA, List<Person> parentsB) {
        for (Person parentA : parentsA) {
            for (Person parentB : parentsB) {
                if (namesMatch(parentA, parentB)) {
                    return parentA;
                }
            }
        }
        return null;
    }

    private String describeNames(List<Person> people) {
        return people.stream().map(personDisplay::englishFullName).collect(Collectors.joining(", "));
    }

    /** Name-equality across English/Nepali in both directions -- see class Javadoc. */
    private boolean namesMatch(Person a, Person b) {
        String aEnglish = personDisplay.englishFullName(a);
        String aNepali = personDisplay.nepaliFullName(a);
        String bEnglish = personDisplay.englishFullName(b);
        String bNepali = personDisplay.nepaliFullName(b);

        return nameMatcher.namesMatch(aEnglish, bEnglish)
                || nameMatcher.namesMatch(aEnglish, bNepali)
                || nameMatcher.namesMatch(aNepali, bEnglish)
                || nameMatcher.namesMatch(aNepali, bNepali);
    }

    private DuplicatePersonSnapshotDto toSnapshot(Person person) {
        return new DuplicatePersonSnapshotDto(
                person.getId(),
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getGender(),
                person.getBirthDate(),
                person.getDeathDate(),
                person.getGenerationNumber(),
                countPopulatedFields(person)
        );
    }

    private int countPopulatedFields(Person person) {
        String[] textFields = {
                person.getFirstName(), person.getFirstNameNepali(), person.getMiddleName(), person.getMiddleNameNepali(),
                person.getLastName(), person.getLastNameNepali(), person.getNickname(), person.getGender(),
                person.getPhotoPath(), person.getBirthPlace(), person.getCurrentAddress(), person.getNotes()
        };
        int count = 0;
        for (String field : textFields) {
            if (field != null && !field.isBlank()) {
                count++;
            }
        }
        if (person.getBirthDate() != null) {
            count++;
        }
        if (person.getDeathDate() != null) {
            count++;
        }
        return count;
    }
}
