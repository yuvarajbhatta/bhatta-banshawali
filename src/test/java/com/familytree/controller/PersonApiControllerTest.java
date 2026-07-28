package com.familytree.controller;

import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipService;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonApiControllerTest {

    @Mock
    private PersonService personService;

    @Mock
    private RelationshipService relationshipService;

    private PersonApiController controller() {
        return new PersonApiController(personService, new PersonProfileAssembler(relationshipService, new PersonDisplayHelper()));
    }

    @Test
    void searchReturnsSummariesForMatchingPersons() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        when(personService.searchPersons("Yuva")).thenReturn(List.of(person));

        List<PersonSummaryDto> results = controller().search("Yuva");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).englishFullName()).isEqualTo("Yuva Bhatta");
    }

    @Test
    void searchWithNoKeywordDelegatesToServiceAsIs() {
        when(personService.searchPersons(null)).thenReturn(List.of());

        List<PersonSummaryDto> results = controller().search(null);

        assertThat(results).isEmpty();
    }

    @Test
    void detailReturnsFullPersonWithFamilySnapshot() {
        Person person = new Person();
        person.setId(7L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        when(personService.getPersonById(7L)).thenReturn(person);
        when(relationshipService.getRelationshipsByPersonAndType(person, com.familytree.entity.RelationshipType.FATHER))
                .thenReturn(List.of());
        when(relationshipService.getRelationshipsByPersonAndType(person, com.familytree.entity.RelationshipType.MOTHER))
                .thenReturn(List.of());
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of());

        PersonDetailDto detail = controller().detail(7L);

        assertThat(detail.id()).isEqualTo(7L);
        assertThat(detail.englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(detail.family()).isNotNull();
    }

    @Test
    void detailThrows404WhenPersonDoesNotExist() {
        when(personService.getPersonById(999L)).thenThrow(new RuntimeException("Person not found with id: 999"));

        assertThatThrownBy(() -> controller().detail(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Person not found");
    }
}
