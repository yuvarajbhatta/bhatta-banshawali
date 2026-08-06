package com.familytree.services;

import com.familytree.config.AppProperties;
import com.familytree.entity.Person;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonPhotoRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserPersonLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PersonPhotoRepository personPhotoRepository;

    @Mock
    private PersonCorrectionRequestRepository personCorrectionRequestRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private PhotoStorageService photoStorageService;

    private PersonService personService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getLineage().setDefaultLastName("Bhatta");
        appProperties.getLineage().setDefaultGender("Male");
        personService = new PersonService(personRepository, relationshipService, nameTransliterationService,
                auditLogService, personPhotoRepository, personCorrectionRequestRepository, userPersonLinkRepository,
                photoStorageService, appProperties);
        lenient().when(personPhotoRepository.findByPersonIdOrderByUploadedAtDesc(any())).thenReturn(List.of());
        lenient().when(personCorrectionRequestRepository.findByPersonId(any())).thenReturn(List.of());
        lenient().when(userPersonLinkRepository.findByPersonId(any())).thenReturn(List.of());
    }

    @Test
    void savePersonNormalizesManualFieldsWithoutAutoTransliteration() {
        Person person = createPerson(1L, " Yuva ");
        person.setFirstNameNepali(" युवा ");
        person.setMiddleName(" ");
        person.setNickname(" Yuva Dai ");
        person.setBirthPlace(" Kispang ");
        when(personRepository.save(person)).thenReturn(person);

        Person saved = personService.savePerson(person);

        assertThat(saved).isEqualTo(person);
        assertThat(person.getFirstName()).isEqualTo("Yuva");
        assertThat(person.getFirstNameNepali()).isEqualTo("युवा");
        assertThat(person.getMiddleName()).isNull();
        assertThat(person.getNickname()).isEqualTo("Yuva Dai");
        assertThat(person.getBirthPlace()).isEqualTo("Kispang");
    }

    @Test
    void updatePersonCopiesAllMutableFields() {
        Person existing = createPerson(1L, "Old");
        Person updated = createPerson(null, "New");
        updated.setMiddleName("Middle");
        updated.setFirstNameNepali("न्यु");
        updated.setMiddleNameNepali("मिडल");
        updated.setLastNameNepali("भट्ट");
        updated.setNickname("N");
        updated.setGenerationNumber(3);
        updated.setGender("Female");
        updated.setBirthDate(LocalDate.of(2000, 1, 1));
        updated.setDeathDate(LocalDate.of(2070, 1, 1));
        updated.setBirthPlace("Kathmandu");
        updated.setCurrentAddress("Lalitpur");
        updated.setPhotoPath("/uploads/persons/new.jpg");
        updated.setNotes("Updated note");
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.save(existing)).thenReturn(existing);

        Person result = personService.updatePerson(1L, updated);

        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getFirstNameNepali()).isEqualTo("न्यु");
        assertThat(result.getMiddleName()).isEqualTo("Middle");
        assertThat(result.getMiddleNameNepali()).isEqualTo("मिडल");
        assertThat(result.getLastNameNepali()).isEqualTo("भट्ट");
        assertThat(result.getNickname()).isEqualTo("N");
        assertThat(result.getGenerationNumber()).isEqualTo(3);
        assertThat(result.getGender()).isEqualTo("Female");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(result.getDeathDate()).isEqualTo(LocalDate.of(2070, 1, 1));
        assertThat(result.getBirthPlace()).isEqualTo("Kathmandu");
        assertThat(result.getCurrentAddress()).isEqualTo("Lalitpur");
        assertThat(result.getPhotoPath()).isEqualTo("/uploads/persons/new.jpg");
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
        when(personRepository.findAllByOrderByGenerationNumberAscIdAsc()).thenReturn(persons);

        assertThat(personService.searchPersons("   ")).isEqualTo(persons);
    }

    @Test
    void searchPersonsUsesSearchQueryForKeyword() {
        List<Person> persons = List.of(createPerson(1L, "Yuva"));
        when(personRepository.searchPersons("yu", null)).thenReturn(persons);

        assertThat(personService.searchPersons("yu")).isEqualTo(persons);
    }

    @Test
    void searchPersonsParsesGenerationNumber() {
        List<Person> persons = List.of(createPerson(3L, "Gen"));
        when(personRepository.searchPersons("7", 7)).thenReturn(persons);

        assertThat(personService.searchPersons("7")).isEqualTo(persons);
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

        Person person = personService.saveLineagePerson("Yuva Prasad", 4);

        assertThat(person.getFirstName()).isEqualTo("Yuva");
        assertThat(person.getMiddleName()).isEqualTo("Prasad");
        assertThat(person.getLastName()).isEqualTo("Bhatta");
        assertThat(person.getFirstNameNepali()).isNull();
        assertThat(person.getMiddleNameNepali()).isNull();
        assertThat(person.getLastNameNepali()).isNull();
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

        Person updated = personService.updateLineagePerson(10L, "New Middle", 5);

        assertThat(updated.getFirstName()).isEqualTo("New");
        assertThat(updated.getMiddleName()).isEqualTo("Middle");
        assertThat(updated.getLastName()).isEqualTo("Bhatta");
        assertThat(updated.getFirstNameNepali()).isNull();
        assertThat(updated.getMiddleNameNepali()).isNull();
        assertThat(updated.getLastNameNepali()).isNull();
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

    @Test
    void clearAutogeneratedNepaliNamesRemovesOnlyGeneratedValues() {
        Person generated = createPerson(1L, "Latadev");
        generated.setFirstNameNepali("लअतअदएव");
        generated.setLastNameNepali("भअततअ");

        Person manual = createPerson(2L, "Balkrishna");
        manual.setFirstNameNepali("बालकृष्ण");
        manual.setLastNameNepali("भट्ट");

        when(personRepository.findAll()).thenReturn(Arrays.asList(generated, manual));
        when(nameTransliterationService.transliterate("Latadev")).thenReturn("लअतअदएव");
        when(nameTransliterationService.transliterate("Bhatta")).thenReturn("भअततअ");
        when(nameTransliterationService.transliterate("Balkrishna")).thenReturn("बअलकरइशनअ");

        int updatedCount = personService.clearAutogeneratedNepaliNames();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(generated.getFirstNameNepali()).isNull();
        assertThat(generated.getLastNameNepali()).isNull();
        assertThat(manual.getFirstNameNepali()).isEqualTo("बालकृष्ण");
        assertThat(manual.getLastNameNepali()).isEqualTo("भट्ट");
        verify(personRepository).saveAll(List.of(generated));
    }

    private Person createPerson(Long id, String firstName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }
}
