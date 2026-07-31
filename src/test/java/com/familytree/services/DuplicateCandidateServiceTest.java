package com.familytree.services;

import com.familytree.dto.DuplicateCandidateDto;
import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateCandidateServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private RelationshipService relationshipService;

    private final NameMatcher nameMatcher = new NameMatcher(new NameTransliterationService());
    private final PersonDisplayHelper personDisplay = new PersonDisplayHelper();

    private DuplicateCandidateService service;

    @BeforeEach
    void setUp() {
        service = new DuplicateCandidateService(personRepository, relationshipRepository, relationshipService,
                nameMatcher, personDisplay);
        lenient().when(relationshipRepository.findAll()).thenReturn(List.of());
        lenient().when(relationshipService.getParentsForPerson(any())).thenReturn(List.of());
    }

    @Test
    void crossLanguageNameMatchProducesACandidate() {
        // NameTransliterationService transliterates "Bhatta" to भट्ट specifically
        // (confirmed by NameMatcherTest) -- one person recorded English-only,
        // the other Nepali-only, should still surface as a candidate.
        Person englishOnly = person(1L);
        englishOnly.setLastName("Bhatta");
        Person nepaliOnly = person(2L);
        nepaliOnly.setLastNameNepali("भट्ट");
        when(personRepository.findAll()).thenReturn(List.of(englishOnly, nepaliOnly));

        List<DuplicateCandidateDto> candidates = service.findCandidates();

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).personA().id()).isEqualTo(1L);
        assertThat(candidates.get(0).personB().id()).isEqualTo(2L);
    }

    @Test
    void conflictingBirthDatesDowngradeToLowConfidenceDespiteNameMatch() {
        Person a = person(1L);
        a.setLastName("Bhatta");
        a.setBirthDate(LocalDate.of(1990, 1, 1));
        Person b = person(2L);
        b.setLastName("Bhatta");
        b.setBirthDate(LocalDate.of(1991, 1, 1));
        when(personRepository.findAll()).thenReturn(List.of(a, b));

        List<DuplicateCandidateDto> candidates = service.findCandidates();

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).confidence()).isEqualTo(MatchConfidence.LOW);
        assertThat(candidates.get(0).hasConflict()).isTrue();
    }

    @Test
    void directlyRelatedPairIsExcludedEntirely() {
        Person a = person(1L);
        a.setLastName("Bhatta");
        Person b = person(2L);
        b.setLastName("Bhatta");
        when(personRepository.findAll()).thenReturn(List.of(a, b));

        Relationship spouseLink = new Relationship();
        spouseLink.setPerson(a);
        spouseLink.setRelatedPerson(b);
        spouseLink.setRelationshipType(RelationshipType.SPOUSE);
        when(relationshipRepository.findAll()).thenReturn(List.of(spouseLink));

        List<DuplicateCandidateDto> candidates = service.findCandidates();

        assertThat(candidates).isEmpty();
    }

    @Test
    void unrelatedNamesProduceNoCandidate() {
        Person a = person(1L);
        a.setLastName("Bhatta");
        Person b = person(2L);
        b.setLastName("Sharma");
        when(personRepository.findAll()).thenReturn(List.of(a, b));

        assertThat(service.findCandidates()).isEmpty();
    }

    private Person person(Long id) {
        Person person = new Person();
        person.setId(id);
        return person;
    }
}
