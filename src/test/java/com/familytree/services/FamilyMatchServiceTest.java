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
        assertThat(result.candidatePersonIds()).containsExactly(3L);
    }

    @Test
    void lowConfidenceWhenNoNameCandidatesExist() {
        when(personRepository.findAll()).thenReturn(List.of(person(1L, "Someone", "Else")));

        FamilyMatchResult result = familyMatchService().evaluateMatch(
                new FamilyMatchRequest("Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", null));

        assertThat(result.confidence()).isEqualTo(MatchConfidence.LOW);
        assertThat(result.candidateEvaluations()).isEmpty();
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

    private Person person(Long id, String firstName, String lastName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        return person;
    }
}
