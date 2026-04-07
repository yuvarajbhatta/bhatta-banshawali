package com.familytree.controller;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipControllerTest {

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private PersonService personService;

    @InjectMocks
    private RelationshipController controller;

    @Test
    void showAddRelationshipFormAddsDefaults() {
        Model model = new ExtendedModelMap();
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        when(personService.getAllPersons()).thenReturn(persons);

        String viewName = controller.showAddRelationshipForm(model);

        assertThat(viewName).isEqualTo("add-relationship");
        assertThat(model.getAttribute("persons")).isEqualTo(persons);
        assertThat(model.getAttribute("relationshipTypes")).isEqualTo(RelationshipType.values());
        assertThat(model.getAttribute("editMode")).isEqualTo(false);
    }

    @Test
    void showEditRelationshipFormPopulatesModel() {
        Relationship relationship = new Relationship();
        relationship.setPerson(createPerson(1L, "Yuva"));
        relationship.setRelatedPerson(createPerson(2L, "Bhoj"));
        relationship.setRelationshipType(RelationshipType.FATHER);
        List<Person> persons = List.of(relationship.getPerson(), relationship.getRelatedPerson());
        when(relationshipService.getRelationshipById(7L)).thenReturn(relationship);
        when(personService.getAllPersons()).thenReturn(persons);

        String viewName = controller.showEditRelationshipForm(7L, new ExtendedModelMap());

        assertThat(viewName).isEqualTo("add-relationship");
    }

    @Test
    void saveRelationshipReturnsErrorsWhenInputMissing() {
        Model model = new ExtendedModelMap();
        when(personService.getAllPersons()).thenReturn(List.of());

        String viewName = controller.saveRelationship(null, null, "", model);

        assertThat(viewName).isEqualTo("add-relationship");
        assertThat(model.getAttribute("personIdError")).isEqualTo("Please select a person.");
        assertThat(model.getAttribute("relatedPersonIdError")).isEqualTo("Please select a related person.");
        assertThat(model.getAttribute("relationshipTypeError")).isEqualTo("Please select a relationship type.");
        verify(relationshipService, never()).saveRelationshipWithAutoLinks(any(), any(), any());
    }

    @Test
    void saveRelationshipReturnsErrorForDuplicate() {
        Model model = new ExtendedModelMap();
        Person person = createPerson(1L, "Yuva");
        Person relatedPerson = createPerson(2L, "Bhoj");
        when(personService.getAllPersons()).thenReturn(List.of(person, relatedPerson));
        when(personService.getPersonById(1L)).thenReturn(person);
        when(personService.getPersonById(2L)).thenReturn(relatedPerson);
        when(relationshipService.relationshipExists(person, relatedPerson, RelationshipType.FATHER)).thenReturn(true);

        String viewName = controller.saveRelationship(1L, 2L, "FATHER", model);

        assertThat(viewName).isEqualTo("add-relationship");
        assertThat(model.getAttribute("duplicateRelationshipError")).isEqualTo("This relationship already exists.");
        verify(relationshipService, never()).saveRelationshipWithAutoLinks(any(), any(), any());
    }

    @Test
    void saveRelationshipPersistsValidRelationship() {
        Model model = new ExtendedModelMap();
        Person person = createPerson(1L, "Yuva");
        Person relatedPerson = createPerson(2L, "Bhoj");
        when(personService.getAllPersons()).thenReturn(List.of(person, relatedPerson));
        when(personService.getPersonById(1L)).thenReturn(person);
        when(personService.getPersonById(2L)).thenReturn(relatedPerson);
        when(relationshipService.relationshipExists(person, relatedPerson, RelationshipType.FATHER)).thenReturn(false);

        String viewName = controller.saveRelationship(1L, 2L, "FATHER", model);

        assertThat(viewName).isEqualTo("redirect:/relationships");
        verify(relationshipService).saveRelationshipWithAutoLinks(person, relatedPerson, RelationshipType.FATHER);
    }

    @Test
    void updateRelationshipReturnsErrorsWhenPersonsMatch() {
        Model model = new ExtendedModelMap();
        when(personService.getAllPersons()).thenReturn(List.of());

        String viewName = controller.updateRelationship(5L, 1L, 1L, "SPOUSE", model);

        assertThat(viewName).isEqualTo("add-relationship");
        assertThat(model.getAttribute("samePersonError")).isEqualTo("Person and related person cannot be the same.");
        verify(relationshipService, never()).updateRelationship(any(), any(), any(), any());
    }

    @Test
    void updateRelationshipPersistsValidUpdate() {
        Model model = new ExtendedModelMap();
        Person person = createPerson(1L, "Yuva");
        Person relatedPerson = createPerson(2L, "Bhoj");
        when(personService.getAllPersons()).thenReturn(List.of(person, relatedPerson));
        when(personService.getPersonById(1L)).thenReturn(person);
        when(personService.getPersonById(2L)).thenReturn(relatedPerson);

        String viewName = controller.updateRelationship(5L, 1L, 2L, "FATHER", model);

        assertThat(viewName).isEqualTo("redirect:/relationships");
        verify(relationshipService).updateRelationship(5L, person, relatedPerson, RelationshipType.FATHER);
    }

    @Test
    void listRelationshipsAddsAllRelationships() {
        List<Relationship> relationships = List.of(new Relationship());
        Model model = new ExtendedModelMap();
        when(relationshipService.getAllRelationships()).thenReturn(relationships);

        String viewName = controller.listRelationships(model);

        assertThat(viewName).isEqualTo("relationships");
        assertThat(model.getAttribute("relationships")).isEqualTo(relationships);
    }

    @Test
    void deleteRelationshipRedirectsToList() {
        String viewName = controller.deleteRelationship(12L);

        assertThat(viewName).isEqualTo("redirect:/relationships");
        verify(relationshipService).deleteRelationshipById(12L);
    }

    private Person createPerson(Long id, String firstName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }
}
