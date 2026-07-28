package com.familytree.controller;

import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipService;
import com.familytree.services.ViewerContextResolver;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonApiControllerTest {

    @Mock
    private PersonService personService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private Authentication authentication;

    private PersonApiController controller() {
        PersonProfileAssembler assembler = new PersonProfileAssembler(relationshipService, new PersonDisplayHelper());
        ViewerContextResolver viewerContextResolver = new ViewerContextResolver(userAccountRepository, userPersonLinkRepository);
        return new PersonApiController(personService, assembler, viewerContextResolver);
    }

    private Authentication asAdmin() {
        org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        return authentication;
    }

    @Test
    void searchReturnsSummariesForMatchingPersons() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        when(personService.searchPersons("Yuva")).thenReturn(List.of(person));

        List<PersonSummaryDto> results = controller().search("Yuva", asAdmin());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).englishFullName()).isEqualTo("Yuva Bhatta");
    }

    @Test
    void searchWithNoKeywordDelegatesToServiceAsIs() {
        when(personService.searchPersons(null)).thenReturn(List.of());

        List<PersonSummaryDto> results = controller().search(null, asAdmin());

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

        PersonDetailDto detail = controller().detail(7L, asAdmin());

        assertThat(detail.id()).isEqualTo(7L);
        assertThat(detail.englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(detail.family()).isNotNull();
    }

    @Test
    void detailThrows404WhenPersonDoesNotExist() {
        when(personService.getPersonById(999L)).thenThrow(new RuntimeException("Person not found with id: 999"));

        assertThatThrownBy(() -> controller().detail(999L, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Person not found");
    }
}
