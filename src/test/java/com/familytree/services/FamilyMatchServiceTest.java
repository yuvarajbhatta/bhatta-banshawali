package com.familytree.services;

import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.repository.PersonRepository;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyMatchServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipService relationshipService;

    private final NameMatcher nameMatcher = new NameMatcher(new NameTransliterationService());
    private final PersonDisplayHelper personDisplayHelper = new PersonDisplayHelper();

    private FamilyMatchService familyMatchService() {
        return new FamilyMatchService(personRepository, relationshipService, nameMatcher, personDisplayHelper);
    }

    // --- Strategy A: existing-person (applicant's own name already in the tree) ---

    @Test
    void highConfidenceWhenNameFatherAndGrandfatherAllMatchUniquely() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");
        Person applicantMatch = person(3L, "Yuva", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father, applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(result.existingPersonCandidateIds()).containsExactly(3L);
        assertThat(result.newPersonFatherCandidateIds()).isEmpty();
    }

    @Test
    void lowConfidenceWhenNoNameCandidatesExist() {
        when(personRepository.findAll()).thenReturn(List.of(person(1L, "Someone", "Else")));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.LOW);
        assertThat(result.existingPersonCandidates()).isEmpty();
        assertThat(result.newPersonCandidates()).isEmpty();
    }

    @Test
    void lowConfidenceWhenNameMatchesButFatherDoesNot() {
        Person applicantMatch = person(3L, "Yuva", "Bhatta");
        Person unrelatedFather = person(4L, "Someone", "Unrelated");

        when(personRepository.findAll()).thenReturn(List.of(applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(unrelatedFather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.LOW);
    }

    @Test
    void mediumConfidenceWhenFatherMatchesButGrandfatherDoesNot() {
        Person unrelatedGrandfather = person(1L, "Someone", "Else");
        Person father = person(2L, "Bhoj", "Bhatta");
        Person applicantMatch = person(3L, "Yuva", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(unrelatedGrandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.MEDIUM);
    }

    @Test
    void mediumConfidenceWhenMultipleFullMatchesAreAmbiguous() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");
        Person firstCandidate = person(3L, "Yuva", "Bhatta");
        Person secondCandidate = person(4L, "Yuva", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father, firstCandidate, secondCandidate));
        when(relationshipService.getParentsForPerson(firstCandidate)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(secondCandidate)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.MEDIUM);
    }

    @Test
    void dobConflictPreventsHighConfidenceEvenWithFullLineageMatch() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");
        Person applicantMatch = person(3L, "Yuva", "Bhatta");
        applicantMatch.setBirthDate(LocalDate.of(1990, 1, 1));

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father, applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", LocalDate.of(1995, 6, 15)));

        assertThat(result.confidence()).isNotEqualTo(MatchConfidence.HIGH);
    }

    @Test
    void noDobConflictWhenCandidateHasNoRecordedBirthDate() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");
        Person applicantMatch = person(3L, "Yuva", "Bhatta");
        // birthDate deliberately left null -- historical records are often incomplete.

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father, applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", LocalDate.of(1995, 6, 15)));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);
    }

    @Test
    void doesNotOfferARedundantCreateCandidateWhenTheApplicantAlreadyExistsUnderThatFather() {
        // Strategy A already finds and fully confirms the real applicant --
        // Strategy B must not ALSO independently suggest creating a
        // duplicate new person under the same father, which would otherwise
        // double the fullLineageMatches count and wrongly downgrade this
        // clean, unambiguous case from HIGH to MEDIUM.
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");
        Person applicantMatch = person(3L, "Yuva", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father, applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(result.existingPersonCandidateIds()).containsExactly(3L);
        assertThat(result.newPersonCandidates()).isEmpty();
    }

    // --- Strategy B: new-person (applicant doesn't exist yet, but father does) ---

    @Test
    void highConfidenceViaFatherNameStrategyWhenApplicantDoesNotExistYet() {
        // No Person matches "Yuva Bhatta" at all -- only the father and
        // grandfather are already in the tree. This is the exact gap that
        // motivated adding this strategy: a brand-new applicant whose
        // lineage already exists should still resolve to HIGH.
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(result.existingPersonCandidates()).isEmpty();
        assertThat(result.newPersonFatherCandidateIds()).containsExactly(2L);
    }

    @Test
    void mediumConfidenceWhenMultipleFatherCandidatesAreAmbiguous() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person firstFather = person(2L, "Bhoj", "Bhatta");
        Person secondFather = person(3L, "Bhoj", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, firstFather, secondFather));
        when(relationshipService.getParentsForPerson(firstFather)).thenReturn(List.of(grandfather));
        when(relationshipService.getParentsForPerson(secondFather)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.MEDIUM);
        assertThat(result.newPersonFatherCandidateIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void mediumConfidenceWhenFatherMatchesButGrandfatherDoesNotForNewPersonStrategy() {
        Person unrelatedGrandfather = person(1L, "Someone", "Else");
        Person father = person(2L, "Bhoj", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(unrelatedGrandfather, father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(unrelatedGrandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.MEDIUM);
        assertThat(result.newPersonFatherCandidateIds()).containsExactly(2L);
    }

    @Test
    void fatherWithManyExistingChildrenIsNotExcludedOrPenalized() {
        // A father candidate with several other children already linked
        // into the tree is normal family structure -- this must not affect
        // whether he's offered as a candidate, nor the confidence level.
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(result.newPersonFatherCandidateIds()).containsExactly(2L);
        // The matcher never even asks how many children this father already
        // has -- proving there's no exclusion/penalty check to bypass.
        org.mockito.Mockito.verify(relationshipService, org.mockito.Mockito.never()).getChildrenForPerson(org.mockito.ArgumentMatchers.any());
    }

    // --- Fuzzy matching caps at MEDIUM, never HIGH ---

    @Test
    void fuzzyOnlyMatchCapsAtMediumForExistingPersonStrategy() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhojraj", "Bhatta");
        Person applicantMatch = person(3L, "Yuva", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father, applicantMatch));
        when(relationshipService.getParentsForPerson(applicantMatch)).thenReturn(List.of(father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        // "Bhojaraj Bhatta" is a fuzzy (single-character-insertion) variant of
        // the recorded "Bhojraj Bhatta" -- exact on applicant/grandfather,
        // fuzzy on father.
        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhojraj Bhatta", "Jhanka Bhatta", null));
        // (sanity baseline -- exact match everywhere still produces HIGH; see next test)
        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);

        FamilyMatchResult fuzzyResult = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhojaraj Bhatta", "Jhanka Bhatta", null));

        assertThat(fuzzyResult.confidence()).isEqualTo(MatchConfidence.MEDIUM);
    }

    @Test
    void fuzzyOnlyMatchCapsAtMediumForNewPersonStrategy() {
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhojraj", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhojaraj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.MEDIUM);
        assertThat(result.newPersonFatherCandidateIds()).containsExactly(2L);
    }

    @Test
    void exactMatchOnEveryHopStillProducesHighConfidence() {
        // Regression guard: adding fuzzy support must not accidentally
        // downgrade a case that was already fully exact.
        Person grandfather = person(1L, "Jhanka", "Bhatta");
        Person father = person(2L, "Bhoj", "Bhatta");

        when(personRepository.findAll()).thenReturn(List.of(grandfather, father));
        when(relationshipService.getParentsForPerson(father)).thenReturn(List.of(grandfather));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.HIGH);
    }

    // --- findSpouseMatchingName: corroborating a submitted mother's name via a father's recorded spouse ---

    @Test
    void findSpouseMatchingNameReturnsTheSpouseOnAnExactMatch() {
        Person father = person(2L, "Bhoj", "Bhatta");
        Person spouse = person(741L, "Sita", "Bhatta");
        when(relationshipService.getSpousesForPerson(father)).thenReturn(List.of(spouse));

        assertThat(familyMatchService().findSpouseMatchingName(father, "Sita Bhatta")).contains(spouse);
    }

    @Test
    void findSpouseMatchingNameReturnsTheSpouseOnAFuzzyMatch() {
        Person father = person(2L, "Bhoj", "Bhatta");
        Person spouse = person(741L, "Sita", "Bhatta");
        when(relationshipService.getSpousesForPerson(father)).thenReturn(List.of(spouse));

        // Single-character-insertion variant, same as the fuzzy cases above.
        assertThat(familyMatchService().findSpouseMatchingName(father, "Sitaa Bhatta")).contains(spouse);
    }

    @Test
    void findSpouseMatchingNameIsEmptyWhenTheFatherHasNoRecordedSpouse() {
        Person father = person(2L, "Bhoj", "Bhatta");
        when(relationshipService.getSpousesForPerson(father)).thenReturn(List.of());

        assertThat(familyMatchService().findSpouseMatchingName(father, "Sita Bhatta")).isEmpty();
    }

    @Test
    void findSpouseMatchingNameIsEmptyWhenNoRecordedSpouseNameMatches() {
        Person father = person(2L, "Bhoj", "Bhatta");
        Person unrelatedSpouse = person(741L, "Someone", "Else");
        when(relationshipService.getSpousesForPerson(father)).thenReturn(List.of(unrelatedSpouse));

        assertThat(familyMatchService().findSpouseMatchingName(father, "Sita Bhatta")).isEmpty();
    }

    @Test
    void findSpouseMatchingNamePicksTheMatchingSpouseAmongMultiple() {
        Person father = person(2L, "Bhoj", "Bhatta");
        Person firstWife = person(740L, "Radha", "Bhatta");
        Person secondWife = person(741L, "Sita", "Bhatta");
        when(relationshipService.getSpousesForPerson(father)).thenReturn(List.of(firstWife, secondWife));

        assertThat(familyMatchService().findSpouseMatchingName(father, "Sita Bhatta")).contains(secondWife);
    }

    @Test
    void findSpouseMatchingNameIsEmptyWhenNoMotherNameWasSubmitted() {
        Person father = person(2L, "Bhoj", "Bhatta");

        assertThat(familyMatchService().findSpouseMatchingName(father, null)).isEmpty();
        assertThat(familyMatchService().findSpouseMatchingName(father, "  ")).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(relationshipService);
    }

    private Person person(Long id, String firstName, String lastName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        return person;
    }
}
