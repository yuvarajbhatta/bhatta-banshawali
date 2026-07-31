package com.familytree.services;

import com.familytree.dto.AdminUserAccountDto;
import com.familytree.dto.DataQualityReportDto;
import com.familytree.dto.DateIssueDto;
import com.familytree.dto.ParentGapDto;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataQualityServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserAccountAdminService userAccountAdminService;

    private final PersonDisplayHelper personDisplay = new PersonDisplayHelper();

    private DataQualityService service;

    @BeforeEach
    void setUp() {
        service = new DataQualityService(personRepository, relationshipRepository, relationshipService,
                userAccountAdminService, personDisplay);
        lenient().when(relationshipRepository.findAll()).thenReturn(List.of());
        lenient().when(relationshipService.getParentsForPerson(any())).thenReturn(List.of());
        lenient().when(userAccountAdminService.listAll()).thenReturn(List.of());
    }

    @Test
    void findCyclesDetectsThreeNodeCycleAsOneCycleNotThree() {
        Person a = person(1L, "A");
        Person b = person(2L, "B");
        Person c = person(3L, "C");
        when(personRepository.findAll()).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findAll()).thenReturn(List.of(
                childRelationship(a, b),
                childRelationship(b, c),
                childRelationship(c, a)
        ));

        DataQualityReportDto report = service.buildReport();

        assertThat(report.cycles()).hasSize(1);
        assertThat(Set.copyOf(report.cycles().get(0).personIds())).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void findCyclesIgnoresAnAcyclicChainAlongsideARealCycle() {
        Person x = person(10L, "X");
        Person y = person(11L, "Y");
        Person p = person(20L, "P");
        Person q = person(21L, "Q");
        when(personRepository.findAll()).thenReturn(List.of(x, y, p, q));
        when(relationshipRepository.findAll()).thenReturn(List.of(
                childRelationship(x, y),
                childRelationship(y, x),
                childRelationship(p, q)
        ));

        DataQualityReportDto report = service.buildReport();

        assertThat(report.cycles()).hasSize(1);
        assertThat(Set.copyOf(report.cycles().get(0).personIds())).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void findParentGapsClassifiesByKnownParentCount() {
        Person noParents = person(1L, "NoParents");
        Person oneParent = person(2L, "OneParent");
        Person twoParents = person(3L, "TwoParents");
        Person parent = person(4L, "Parent");
        Person otherParent = person(5L, "OtherParent");
        when(personRepository.findAll()).thenReturn(List.of(noParents, oneParent, twoParents));
        when(relationshipService.getParentsForPerson(noParents)).thenReturn(List.of());
        when(relationshipService.getParentsForPerson(oneParent)).thenReturn(List.of(parent));
        when(relationshipService.getParentsForPerson(twoParents)).thenReturn(List.of(parent, otherParent));

        DataQualityReportDto report = service.buildReport();

        assertThat(report.parentGaps())
                .extracting(ParentGapDto::personId, ParentGapDto::knownParentCount)
                .containsExactlyInAnyOrder(tuple(1L, 0), tuple(2L, 1));
    }

    @Test
    void findDateIssuesFlagsAllFourSubcases() {
        Person missingBirth = person(1L, "MissingBirth");
        Person deathBeforeBirth = person(2L, "DeathBeforeBirth");
        deathBeforeBirth.setBirthDate(LocalDate.of(2000, 1, 1));
        deathBeforeBirth.setDeathDate(LocalDate.of(1999, 1, 1));
        Person futureBirth = person(3L, "FutureBirth");
        futureBirth.setBirthDate(LocalDate.now().plusDays(1));
        Person childOfYoungParent = person(4L, "Child");
        childOfYoungParent.setBirthDate(LocalDate.of(2020, 1, 1));
        Person youngParent = person(5L, "YoungParent");
        youngParent.setBirthDate(LocalDate.of(2015, 1, 1));

        when(personRepository.findAll())
                .thenReturn(List.of(missingBirth, deathBeforeBirth, futureBirth, childOfYoungParent));
        when(relationshipService.getParentsForPerson(childOfYoungParent)).thenReturn(List.of(youngParent));

        DataQualityReportDto report = service.buildReport();

        assertThat(report.dateIssues())
                .extracting(DateIssueDto::personId, DateIssueDto::issueType)
                .containsExactlyInAnyOrder(
                        tuple(1L, DataQualityService.ISSUE_MISSING_BIRTH_DATE),
                        tuple(2L, DataQualityService.ISSUE_DEATH_BEFORE_BIRTH),
                        tuple(3L, DataQualityService.ISSUE_FUTURE_BIRTH_DATE),
                        tuple(4L, DataQualityService.ISSUE_IMPLAUSIBLE_PARENT_AGE_GAP)
                );
    }

    @Test
    void findUnlinkedAccountsFiltersOutLinkedOnes() {
        when(personRepository.findAll()).thenReturn(List.of());
        AdminUserAccountDto linked = new AdminUserAccountDto(1L, "a@example.com", null, null, null, null, false,
                10L, "Linked Person", null, null, null, null, null);
        AdminUserAccountDto unlinked = new AdminUserAccountDto(2L, "b@example.com", null, null, null, null, false,
                null, null, null, null, null, null, null);
        when(userAccountAdminService.listAll()).thenReturn(List.of(linked, unlinked));

        DataQualityReportDto report = service.buildReport();

        assertThat(report.unlinkedAccounts()).containsExactly(unlinked);
    }

    private Person person(Long id, String firstName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }

    private Relationship childRelationship(Person parent, Person child) {
        Relationship relationship = new Relationship();
        relationship.setPerson(parent);
        relationship.setRelatedPerson(child);
        relationship.setRelationshipType(RelationshipType.CHILD);
        return relationship;
    }
}
