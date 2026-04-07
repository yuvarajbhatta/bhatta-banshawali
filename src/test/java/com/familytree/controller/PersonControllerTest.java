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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonService personService;

    @Mock
    private RelationshipService relationshipService;

    @InjectMocks
    private PersonController controller;

    @Test
    void showAddPersonFormInitializesEmptyPerson() {
        Model model = new ExtendedModelMap();

        String viewName = controller.showAddPersonForm(model);

        assertThat(viewName).isEqualTo("add-person");
        assertThat(model.getAttribute("person")).isInstanceOf(Person.class);
    }

    @Test
    void savePersonReturnsFormWhenValidationFails() {
        Person person = new Person();
        BindingResult bindingResult = new BeanPropertyBindingResult(person, "person");
        bindingResult.rejectValue("firstName", "required", "First name is required");

        String viewName = controller.savePerson(person, bindingResult);

        assertThat(viewName).isEqualTo("add-person");
        verify(personService, never()).savePerson(any());
    }

    @Test
    void savePersonRedirectsWhenValidationSucceeds() {
        Person person = createPerson(1L, "Yuva");
        BindingResult bindingResult = new BeanPropertyBindingResult(person, "person");

        String viewName = controller.savePerson(person, bindingResult);

        assertThat(viewName).isEqualTo("redirect:/persons");
        verify(personService).savePerson(person);
    }

    @Test
    void showEditPersonFormAddsPersonAndEditMode() {
        Person person = createPerson(1L, "Yuva");
        Model model = new ExtendedModelMap();
        when(personService.getPersonById(1L)).thenReturn(person);

        String viewName = controller.showEditPersonForm(1L, model);

        assertThat(viewName).isEqualTo("add-person");
        assertThat(model.getAttribute("person")).isEqualTo(person);
        assertThat(model.getAttribute("editMode")).isEqualTo(true);
    }

    @Test
    void updatePersonReturnsFormWhenValidationFails() {
        Person person = createPerson(1L, "Yuva");
        BindingResult bindingResult = new BeanPropertyBindingResult(person, "person");
        bindingResult.reject("error");
        Model model = new ExtendedModelMap();

        String viewName = controller.updatePerson(1L, person, bindingResult, model);

        assertThat(viewName).isEqualTo("add-person");
        assertThat(model.getAttribute("editMode")).isEqualTo(true);
        verify(personService, never()).updatePerson(any(), any());
    }

    @Test
    void updatePersonRedirectsWhenValidationSucceeds() {
        Person person = createPerson(1L, "Yuva");
        BindingResult bindingResult = new BeanPropertyBindingResult(person, "person");
        Model model = new ExtendedModelMap();

        String viewName = controller.updatePerson(1L, person, bindingResult, model);

        assertThat(viewName).isEqualTo("redirect:/persons");
        verify(personService).updatePerson(1L, person);
    }

    @Test
    void deletePersonRedirectsToList() {
        String viewName = controller.deletePerson(5L);

        assertThat(viewName).isEqualTo("redirect:/persons");
        verify(personService).deletePersonById(5L);
    }

    @Test
    void listPersonsAddsResultsAndKeyword() {
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        Model model = new ExtendedModelMap();
        when(personService.searchPersons("yu")).thenReturn(persons);

        String viewName = controller.listPersons("yu", model);

        assertThat(viewName).isEqualTo("persons");
        assertThat(model.getAttribute("persons")).isEqualTo(persons);
        assertThat(model.getAttribute("keyword")).isEqualTo("yu");
    }

    @Test
    void viewPersonAddsRelationshipsToModel() {
        Person person = createPerson(1L, "Yuva");
        Relationship fatherRelationship = new Relationship();
        fatherRelationship.setRelatedPerson(createPerson(2L, "Bhoj"));
        List<Relationship> fathers = List.of(fatherRelationship);
        List<Relationship> mothers = List.of();
        List<Person> spouses = List.of(createPerson(3L, "Mina"));
        List<Person> children = List.of(createPerson(4L, "Child"));
        Model model = new ExtendedModelMap();

        when(personService.getPersonById(1L)).thenReturn(person);
        when(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)).thenReturn(fathers);
        when(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER)).thenReturn(mothers);
        when(relationshipService.getSpousesForPerson(person)).thenReturn(spouses);
        when(relationshipService.getChildrenForPerson(person)).thenReturn(children);

        String viewName = controller.viewPerson(1L, model);

        assertThat(viewName).isEqualTo("person-details");
        assertThat(model.getAttribute("person")).isEqualTo(person);
        assertThat(model.getAttribute("fathers")).isEqualTo(fathers);
        assertThat(model.getAttribute("mothers")).isEqualTo(mothers);
        assertThat(model.getAttribute("spouses")).isEqualTo(spouses);
        assertThat(model.getAttribute("children")).isEqualTo(children);
    }

    @Test
    void viewGenerationsAddsOrderedPersons() {
        List<Person> persons = List.of(createPerson(1L, "Root"));
        Model model = new ExtendedModelMap();
        when(personService.getAllPersonsOrderedByGeneration()).thenReturn(persons);

        String viewName = controller.viewGenerations(model);

        assertThat(viewName).isEqualTo("generations");
        assertThat(model.getAttribute("persons")).isEqualTo(persons);
    }

    @Test
    void viewLineageReturnsLineageView() {
        String viewName = controller.viewLineage(new ExtendedModelMap());

        assertThat(viewName).isEqualTo("lineage");
    }

    @Test
    void getLineageTreeReturnsEmptyMapWhenNoRootExists() {
        when(relationshipService.getRootPersonForLineage()).thenReturn(null);
        when(relationshipService.buildLineageTree(null)).thenReturn(null);

        var response = controller.getLineageTree();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new LinkedHashMap<>());
    }

    @Test
    void getLineageTreeReturnsTreeWhenRootExists() {
        Person root = createPerson(1L, "Root");
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("dbId", 1L);
        when(relationshipService.getRootPersonForLineage()).thenReturn(root);
        when(relationshipService.buildLineageTree(root)).thenReturn(tree);

        var response = controller.getLineageTree();

        assertThat(response.getBody()).isEqualTo(tree);
    }

    @Test
    void saveLineagePersonCreatesNewPersonAndParentRelationship() {
        Person parent = createPerson(10L, "Parent");
        Person child = createPerson(11L, "Child");
        child.setMiddleName("Middle");
        child.setGenerationNumber(2);

        when(personService.saveLineagePerson("Child Middle", 2)).thenReturn(child);
        when(personService.getPersonById(10L)).thenReturn(parent);

        var response = controller.saveLineagePerson("Child Middle", null, 10L, 2);

        assertThat(response.getBody()).containsEntry("id", 11L);
        assertThat(response.getBody()).containsEntry("firstName", "Child");
        assertThat(response.getBody()).containsEntry("middleName", "Middle");
        verify(relationshipService).saveRelationshipWithAutoLinks(parent, child, RelationshipType.CHILD);
    }

    @Test
    void saveLineagePersonUpdatesExistingPersonWhenPersonIdProvided() {
        Person person = createPerson(11L, "Updated");
        person.setGenerationNumber(3);
        when(personService.updateLineagePerson(11L, "Updated Name", 3)).thenReturn(person);

        var response = controller.saveLineagePerson("Updated Name", 11L, 99L, 3);

        assertThat(response.getBody()).containsEntry("id", 11L);
        verify(personService).updateLineagePerson(11L, "Updated Name", 3);
        verify(relationshipService, never()).saveRelationshipWithAutoLinks(any(), any(), any());
    }

    private Person createPerson(Long id, String firstName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }
}
