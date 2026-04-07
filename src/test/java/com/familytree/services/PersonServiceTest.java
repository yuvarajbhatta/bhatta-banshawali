package com.familytree.services;

import com.familytree.config.AppProperties;
import com.familytree.entity.Person;
import com.familytree.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private NameTransliterationService nameTransliterationService;

    private PersonService personService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getLineage().setDefaultLastName("Bhatta");
        appProperties.getLineage().setDefaultGender("Male");
        personService = new PersonService(personRepository, relationshipService, nameTransliterationService, appProperties);
    }

    @Test
    void savePersonDelegatesToRepository() {
        Person person = createPerson(1L, "Yuva");
        when(nameTransliterationService.transliterate("Yuva")).thenReturn("युवा");
        when(nameTransliterationService.transliterate("Bhatta")).thenReturn("भट्ट");
        when(personRepository.save(person)).thenReturn(person);

        Person saved = personService.savePerson(person);

        assertThat(saved).isEqualTo(person);
        assertThat(person.getFirstNameNepali()).isEqualTo("युवा");
        assertThat(person.getLastNameNepali()).isEqualTo("भट्ट");
    }

    @Test
    void updatePersonCopiesAllMutableFields() {
        Person existing = createPerson(1L, "Old");
        Person updated = createPerson(null, "New");
        updated.setMiddleName("Middle");
        updated.setFirstNameNepali("न्यु");
        updated.setGenerationNumber(3);
        updated.setGender("Female");
        updated.setBirthDate(LocalDate.of(2000, 1, 1));
        updated.setDeathDate(LocalDate.of(2070, 1, 1));
        updated.setNotes("Updated note");
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.save(existing)).thenReturn(existing);
        when(nameTransliterationService.transliterate("Middle")).thenReturn("मिडल");
        when(nameTransliterationService.transliterate("Bhatta")).thenReturn("भट्ट");

        Person result = personService.updatePerson(1L, updated);

        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getFirstNameNepali()).isEqualTo("न्यु");
        assertThat(result.getMiddleName()).isEqualTo("Middle");
        assertThat(result.getMiddleNameNepali()).isEqualTo("मिडल");
        assertThat(result.getGenerationNumber()).isEqualTo(3);
        assertThat(result.getGender()).isEqualTo("Female");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(result.getDeathDate()).isEqualTo(LocalDate.of(2070, 1, 1));
        assertThat(result.getNotes()).isEqualTo("Updated note");
    }

    @Test
    void updatePersonThrowsWhenPersonMissing() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.updatePerson(99L, createPerson(null, "Test")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Person not found with id: 99");
    }

    @Test
    void getAllPersonsReturnsRepositoryResults() {
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        when(personRepository.findAll()).thenReturn(persons);

        assertThat(personService.getAllPersons()).isEqualTo(persons);
    }

    @Test
    void searchPersonsUsesFindAllForBlankKeyword() {
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        when(personRepository.findAll()).thenReturn(persons);

        assertThat(personService.searchPersons("   ")).isEqualTo(persons);
    }

    @Test
    void searchPersonsUsesSearchQueryForKeyword() {
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        when(personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrFirstNameNepaliContainingIgnoreCaseOrLastNameNepaliContainingIgnoreCase("yu", "yu", "yu", "yu"))
                .thenReturn(persons);

        assertThat(personService.searchPersons("yu")).isEqualTo(persons);
    }

    @Test
    void getPersonByIdReturnsExistingPerson() {
        Person person = createPerson(1L, "Yuva");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        assertThat(personService.getPersonById(1L)).isEqualTo(person);
    }

    @Test
    void deletePersonByIdDeletesRelationshipsBeforePerson() {
        Person person = createPerson(1L, "Yuva");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        personService.deletePersonById(1L);

        InOrder inOrder = inOrder(relationshipService, personRepository);
        inOrder.verify(relationshipService).deleteRelationshipsByPerson(person);
        inOrder.verify(personRepository).delete(person);
    }

    @Test
    void getAllPersonsOrderedByGenerationDelegatesToRepository() {
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        when(personRepository.findAllByOrderByGenerationNumberAscIdAsc()).thenReturn(persons);

        assertThat(personService.getAllPersonsOrderedByGeneration()).isEqualTo(persons);
    }

    @Test
    void saveLineagePersonParsesNameAndAppliesDefaults() {
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nameTransliterationService.transliterate("Yuva")).thenReturn("युवा");
        when(nameTransliterationService.transliterate("Prasad")).thenReturn("प्रसाद");
        when(nameTransliterationService.transliterate("Bhatta")).thenReturn("भट्ट");

        Person person = personService.saveLineagePerson("Yuva Prasad", 4);

        assertThat(person.getFirstName()).isEqualTo("Yuva");
        assertThat(person.getMiddleName()).isEqualTo("Prasad");
        assertThat(person.getLastName()).isEqualTo("Bhatta");
        assertThat(person.getFirstNameNepali()).isEqualTo("युवा");
        assertThat(person.getMiddleNameNepali()).isEqualTo("प्रसाद");
        assertThat(person.getLastNameNepali()).isEqualTo("भट्ट");
        assertThat(person.getGender()).isEqualTo("Male");
        assertThat(person.getGenerationNumber()).isEqualTo(4);
    }

    @Test
    void saveLineagePersonRejectsBlankName() {
        assertThatThrownBy(() -> personService.saveLineagePerson("   ", 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Full name is required");
    }

    @Test
    void updateLineagePersonUpdatesParsedFields() {
        Person existing = createPerson(10L, "Old");
        when(personRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(personRepository.save(existing)).thenReturn(existing);
        when(nameTransliterationService.transliterate("New")).thenReturn("न्यू");
        when(nameTransliterationService.transliterate("Middle")).thenReturn("मिडल");
        when(nameTransliterationService.transliterate("Bhatta")).thenReturn("भट्ट");

        Person updated = personService.updateLineagePerson(10L, "New Middle", 5);

        assertThat(updated.getFirstName()).isEqualTo("New");
        assertThat(updated.getMiddleName()).isEqualTo("Middle");
        assertThat(updated.getLastName()).isEqualTo("Bhatta");
        assertThat(updated.getFirstNameNepali()).isEqualTo("न्यू");
        assertThat(updated.getMiddleNameNepali()).isEqualTo("मिडल");
        assertThat(updated.getLastNameNepali()).isEqualTo("भट्ट");
        assertThat(updated.getGender()).isEqualTo("Male");
        assertThat(updated.getGenerationNumber()).isEqualTo(5);
    }

    @Test
    void updateLineagePersonThrowsWhenPersonMissing() {
        when(personRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.updateLineagePerson(10L, "Name", 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Person not found with id: 10");
    }

    @Test
    void backfillMissingNepaliNamesUpdatesExistingRowsWithoutNepaliValues() {
        Person missing = createPerson(1L, "Yuva");
        missing.setMiddleName("Prasad");
        Person existing = createPerson(2L, "Bhoj");
        existing.setFirstNameNepali("भोज");
        existing.setLastNameNepali("भट्ट");

        when(personRepository.findAll()).thenReturn(Arrays.asList(missing, existing));
        when(nameTransliterationService.transliterate("Yuva")).thenReturn("युवा");
        when(nameTransliterationService.transliterate("Prasad")).thenReturn("प्रसाद");
        when(nameTransliterationService.transliterate("Bhatta")).thenReturn("भट्ट");

        int updatedCount = personService.backfillMissingNepaliNames();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(missing.getFirstNameNepali()).isEqualTo("युवा");
        assertThat(missing.getMiddleNameNepali()).isEqualTo("प्रसाद");
        assertThat(missing.getLastNameNepali()).isEqualTo("भट्ट");
        verify(personRepository).saveAll(List.of(missing));
    }

    private Person createPerson(Long id, String firstName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }
}
