package com.familytree.controller;

import com.familytree.dto.AdminRelationshipDto;
import com.familytree.dto.AdminRelationshipRequestDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipCycleException;
import com.familytree.services.RelationshipService;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRelationshipApiControllerTest {

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private PersonService personService;

    private AdminRelationshipApiController controller() {
        return new AdminRelationshipApiController(relationshipService, personService, new PersonDisplayHelper());
    }

    private Person person(long id, String first) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(first);
        person.setLastName("Bhatta");
        return person;
    }

    private AdminRelationshipRequestDto request(Long personId, Long relatedPersonId, RelationshipType type) {
        AdminRelationshipRequestDto request = new AdminRelationshipRequestDto();
        request.setPersonId(personId);
        request.setRelatedPersonId(relatedPersonId);
        request.setRelationshipType(type);
        return request;
    }

    @Test
    void listMapsAllRelationships() {
        Relationship relationship = new Relationship();
        relationship.setId(1L);
        relationship.setPerson(person(1L, "Yuva"));
        relationship.setRelatedPerson(person(2L, "Bhoj"));
        relationship.setRelationshipType(RelationshipType.FATHER);
        when(relationshipService.getAllRelationships()).thenReturn(List.of(relationship));

        List<AdminRelationshipDto> results = controller().list();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).personName()).isEqualTo("Yuva Bhatta");
        assertThat(results.get(0).relationshipType()).isEqualTo(RelationshipType.FATHER);
    }

    @Test
    void createRejectsSamePerson() {
        Person p = person(1L, "Yuva");
        when(personService.getPersonById(1L)).thenReturn(p);

        assertThatThrownBy(() -> controller().create(request(1L, 1L, RelationshipType.SPOUSE)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("themselves");
    }

    @Test
    void createRejectsDuplicate() {
        Person p1 = person(1L, "Yuva");
        Person p2 = person(2L, "Bhoj");
        when(personService.getPersonById(1L)).thenReturn(p1);
        when(personService.getPersonById(2L)).thenReturn(p2);
        when(relationshipService.relationshipExists(p1, p2, RelationshipType.FATHER)).thenReturn(true);

        assertThatThrownBy(() -> controller().create(request(1L, 2L, RelationshipType.FATHER)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createRejectsCycle() {
        Person p1 = person(1L, "Yuva");
        Person p2 = person(2L, "Bhoj");
        when(personService.getPersonById(1L)).thenReturn(p1);
        when(personService.getPersonById(2L)).thenReturn(p2);
        when(relationshipService.relationshipExists(p1, p2, RelationshipType.FATHER)).thenReturn(false);
        doThrow(new RelationshipCycleException("would create a cycle"))
                .when(relationshipService).saveRelationshipWithAutoLinks(p1, p2, RelationshipType.FATHER);

        assertThatThrownBy(() -> controller().create(request(1L, 2L, RelationshipType.FATHER)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void createSucceedsAndReReadsTheSavedRow() {
        Person p1 = person(1L, "Yuva");
        Person p2 = person(2L, "Bhoj");
        when(personService.getPersonById(1L)).thenReturn(p1);
        when(personService.getPersonById(2L)).thenReturn(p2);
        when(relationshipService.relationshipExists(p1, p2, RelationshipType.FATHER)).thenReturn(false);

        Relationship saved = new Relationship();
        saved.setId(9L);
        saved.setPerson(p1);
        saved.setRelatedPerson(p2);
        saved.setRelationshipType(RelationshipType.FATHER);
        when(relationshipService.getRelationshipsByPersonAndType(p1, RelationshipType.FATHER)).thenReturn(List.of(saved));

        ResponseEntity<AdminRelationshipDto> response = controller().create(request(1L, 2L, RelationshipType.FATHER));

        verify(relationshipService).saveRelationshipWithAutoLinks(p1, p2, RelationshipType.FATHER);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().id()).isEqualTo(9L);
    }

    @Test
    void updateThrows404WhenRelationshipMissing() {
        when(relationshipService.getRelationshipById(999L)).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> controller().update(999L, request(1L, 2L, RelationshipType.FATHER)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteThrows404WhenRelationshipMissing() {
        when(relationshipService.getRelationshipById(999L)).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> controller().delete(999L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteRemovesExistingRelationship() {
        Relationship existing = new Relationship();
        existing.setId(3L);
        when(relationshipService.getRelationshipById(3L)).thenReturn(existing);

        ResponseEntity<Void> response = controller().delete(3L);

        verify(relationshipService).deleteRelationshipById(3L);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
