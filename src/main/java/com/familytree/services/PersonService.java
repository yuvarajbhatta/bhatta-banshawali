package com.familytree.services;

import com.familytree.config.AppProperties;
import com.familytree.entity.Person;
import com.familytree.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final RelationshipService relationshipService;
    private final NameTransliterationService nameTransliterationService;
    private final String lineageDefaultLastName;
    private final String lineageDefaultGender;

    public PersonService(PersonRepository personRepository,
                         RelationshipService relationshipService,
                         NameTransliterationService nameTransliterationService,
                         AppProperties appProperties) {
        this.personRepository = personRepository;
        this.relationshipService = relationshipService;
        this.nameTransliterationService = nameTransliterationService;
        this.lineageDefaultLastName = normalize(appProperties.getLineage().getDefaultLastName());
        this.lineageDefaultGender = normalize(appProperties.getLineage().getDefaultGender());
    }

    public Person savePerson(Person person) {
        populateNepaliNames(person);
        return personRepository.save(person);
    }

    public Person updatePerson(Long id, Person updatedPerson) {
        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));

        existingPerson.setGenerationNumber(updatedPerson.getGenerationNumber());
        existingPerson.setFirstName(updatedPerson.getFirstName());
        existingPerson.setFirstNameNepali(updatedPerson.getFirstNameNepali());
        existingPerson.setMiddleName(updatedPerson.getMiddleName());
        existingPerson.setMiddleNameNepali(updatedPerson.getMiddleNameNepali());
        existingPerson.setLastName(updatedPerson.getLastName());
        existingPerson.setLastNameNepali(updatedPerson.getLastNameNepali());
        existingPerson.setGender(updatedPerson.getGender());
        existingPerson.setBirthDate(updatedPerson.getBirthDate());
        existingPerson.setDeathDate(updatedPerson.getDeathDate());
        existingPerson.setNotes(updatedPerson.getNotes());
        populateNepaliNames(existingPerson);

        return personRepository.save(existingPerson);
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
    public List<Person> searchPersons(String keyword){
        if (keyword == null || keyword.isBlank()) {
            return personRepository.findAll();
        }
        return personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrFirstNameNepaliContainingIgnoreCaseOrLastNameNepaliContainingIgnoreCase(
                keyword, keyword, keyword, keyword
        );
    }
    public Person getPersonById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));
    }
    public void deletePersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));

        // delete all relationships first
        relationshipService.deleteRelationshipsByPerson(person);

        // then delete person
        personRepository.delete(person);
    }
    public List<Person> getAllPersonsOrderedByGeneration(){
        return personRepository.findAllByOrderByGenerationNumberAscIdAsc();
    }

    public Person saveLineagePerson(String fullName, Integer generationNumber){
        String cleanedName = fullName == null ? "" : fullName.trim();
        if (cleanedName.isEmpty()){
            throw new RuntimeException("Full name is required");
        }
        String[] parts = cleanedName.split("\\s+");

        String firstName = parts.length > 0 ? parts[0] : "";
        String middleName = parts.length > 1 ? parts[1] : null;

        Person person = new Person();
        person.setFirstName(firstName);
        person.setMiddleName(middleName);
        applyLineageDefaults(person);
        person.setGenerationNumber(generationNumber);
        populateNepaliNames(person);

        return personRepository.save(person);
    }

    public Person updateLineagePerson(Long id, String fullName, Integer generationNumber) {
        String cleanedName = fullName == null ? "" : fullName.trim();
        if (cleanedName.isEmpty()) {
            throw new RuntimeException("Full name is required");
        }

        String[] parts = cleanedName.split("\\s+");

        String firstName = parts.length > 0 ? parts[0] : "";
        String middleName = parts.length > 1 ? parts[1] : null;

        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));

        existingPerson.setFirstName(firstName);
        existingPerson.setMiddleName(middleName);
        applyLineageDefaults(existingPerson);
        existingPerson.setGenerationNumber(generationNumber);
        populateNepaliNames(existingPerson);

        return personRepository.save(existingPerson);
    }

    public Person suggestNepaliNames(Person person) {
        Person suggestion = new Person();
        suggestion.setFirstName(normalize(person.getFirstName()));
        suggestion.setMiddleName(normalize(person.getMiddleName()));
        suggestion.setLastName(normalize(person.getLastName()));
        suggestion.setFirstNameNepali(suggestNepaliValue(person.getFirstName(), person.getFirstNameNepali()));
        suggestion.setMiddleNameNepali(suggestNepaliValue(person.getMiddleName(), person.getMiddleNameNepali()));
        suggestion.setLastNameNepali(suggestNepaliValue(person.getLastName(), person.getLastNameNepali()));
        return suggestion;
    }

    public int backfillMissingNepaliNames() {
        List<Person> persons = personRepository.findAll();
        List<Person> updatedPersons = new ArrayList<>();

        for (Person person : persons) {
            boolean changed = false;

            if (normalizeToNull(person.getFirstNameNepali()) == null && normalizeToNull(person.getFirstName()) != null) {
                person.setFirstNameNepali(suggestNepaliValue(person.getFirstName(), null));
                changed = true;
            }
            if (normalizeToNull(person.getMiddleNameNepali()) == null && normalizeToNull(person.getMiddleName()) != null) {
                person.setMiddleNameNepali(suggestNepaliValue(person.getMiddleName(), null));
                changed = true;
            }
            if (normalizeToNull(person.getLastNameNepali()) == null && normalizeToNull(person.getLastName()) != null) {
                person.setLastNameNepali(suggestNepaliValue(person.getLastName(), null));
                changed = true;
            }

            if (changed) {
                updatedPersons.add(person);
            }
        }

        if (!updatedPersons.isEmpty()) {
            personRepository.saveAll(updatedPersons);
        }

        return updatedPersons.size();
    }

    private void applyLineageDefaults(Person person) {
        person.setLastName(lineageDefaultLastName.isBlank() ? null : lineageDefaultLastName);
        person.setGender(lineageDefaultGender.isBlank() ? null : lineageDefaultGender);
    }

    private void populateNepaliNames(Person person) {
        person.setFirstName(normalizeToNull(person.getFirstName()));
        person.setMiddleName(normalizeToNull(person.getMiddleName()));
        person.setLastName(normalizeToNull(person.getLastName()));
        person.setFirstNameNepali(suggestNepaliValue(person.getFirstName(), person.getFirstNameNepali()));
        person.setMiddleNameNepali(suggestNepaliValue(person.getMiddleName(), person.getMiddleNameNepali()));
        person.setLastNameNepali(suggestNepaliValue(person.getLastName(), person.getLastNameNepali()));
    }

    private String suggestNepaliValue(String englishValue, String nepaliValue) {
        String explicitNepali = normalizeToNull(nepaliValue);
        if (explicitNepali != null) {
            return explicitNepali;
        }

        String normalizedEnglish = normalizeToNull(englishValue);
        if (normalizedEnglish == null) {
            return null;
        }

        String transliterated = nameTransliterationService.transliterate(normalizedEnglish);
        return transliterated == null || transliterated.isBlank() ? null : transliterated;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeToNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

}
