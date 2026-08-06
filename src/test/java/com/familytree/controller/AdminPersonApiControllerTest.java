package com.familytree.controller;

import com.familytree.dto.AdminPersonCreateResultDto;
import com.familytree.dto.AdminPersonDto;
import com.familytree.dto.AdminPersonRequestDto;
import com.familytree.entity.Person;
import com.familytree.services.DuplicateCandidateService;
import com.familytree.services.PersonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPersonApiControllerTest {

    @Mock
    private PersonService personService;

    @Mock
    private DuplicateCandidateService duplicateCandidateService;

    private AdminPersonApiController controller() {
        return new AdminPersonApiController(personService, duplicateCandidateService);
    }

    private Person person(long id) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        return person;
    }

    private AdminPersonRequestDto request() {
        AdminPersonRequestDto request = new AdminPersonRequestDto();
        request.setFirstName("Yuva");
        request.setLastName("Bhatta");
        return request;
    }

    @Test
    void listDelegatesToSearch() {
        when(personService.searchPersons("Bhatta")).thenReturn(List.of(person(1L)));

        List<AdminPersonDto> results = controller().list("Bhatta");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).firstName()).isEqualTo("Yuva");
    }

    @Test
    void detailReturns404WhenPersonMissing() {
        when(personService.getPersonById(999L)).thenThrow(new RuntimeException("Person not found with id: 999"));

        assertThatThrownBy(() -> controller().detail(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Person not found");
    }

    @Test
    void createSavesAndReturns201() {
        when(personService.savePerson(any(Person.class))).thenReturn(person(5L));

        ResponseEntity<AdminPersonCreateResultDto> response = controller().create(request());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().person().id()).isEqualTo(5L);
        assertThat(response.getBody().possibleDuplicates()).isEmpty();
    }

    @Test
    void createSurfacesPossibleDuplicatesWithoutBlockingTheSave() {
        when(personService.savePerson(any(Person.class))).thenReturn(person(5L));
        when(duplicateCandidateService.findLikelyDuplicatesOf(any(Person.class))).thenReturn(List.of(person(9L)));

        ResponseEntity<AdminPersonCreateResultDto> response = controller().create(request());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().person().id()).isEqualTo(5L);
        assertThat(response.getBody().possibleDuplicates()).hasSize(1);
        assertThat(response.getBody().possibleDuplicates().get(0).id()).isEqualTo(9L);
    }

    @Test
    void updateThrows404WhenPersonMissing() {
        when(personService.getPersonById(42L)).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> controller().update(42L, request()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateDelegatesToServiceWhenPersonExists() {
        when(personService.getPersonById(7L)).thenReturn(person(7L));
        when(personService.updatePerson(eq(7L), any(Person.class))).thenReturn(person(7L));

        AdminPersonDto result = controller().update(7L, request());

        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void deleteRemovesExistingPerson() {
        when(personService.getPersonById(3L)).thenReturn(person(3L));

        ResponseEntity<Void> response = controller().delete(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void deleteThrows404WhenPersonMissing() {
        when(personService.getPersonById(404L)).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> controller().delete(404L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
