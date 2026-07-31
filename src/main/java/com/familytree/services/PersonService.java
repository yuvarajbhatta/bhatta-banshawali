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
    private final AuditLogService auditLogService;
    private final String lineageDefaultLastName;
    private final String lineageDefaultGender;

    public PersonService(PersonRepository personRepository,
                         RelationshipService relationshipService,
                         NameTransliterationService nameTransliterationService,
                         AuditLogService auditLogService,
                         AppProperties appProperties) {
        this.personRepository = personRepository;
        this.relationshipService = relationshipService;
        this.nameTransliterationService = nameTransliterationService;
        this.auditLogService = auditLogService;
        this.lineageDefaultLastName = normalize(appProperties.getLineage().getDefaultLastName());
        this.lineageDefaultGender = normalize(appProperties.getLineage().getDefaultGender());
    }

    public Person savePerson(Person person) {
        normalizePersonFields(person);
        Person saved = personRepository.save(person);
        auditLogService.record(AuditLogService.ACTION_PERSON_CREATED, AuditLogService.ENTITY_PERSON, saved.getId(),
                "Created person " + fullNameFor(saved));
        return saved;
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
        existingPerson.setNickname(updatedPerson.getNickname());
        existingPerson.setGender(updatedPerson.getGender());
        existingPerson.setBirthDate(updatedPerson.getBirthDate());
        existingPerson.setDeathDate(updatedPerson.getDeathDate());
        existingPerson.setPhotoPath(updatedPerson.getPhotoPath());
        existingPerson.setBirthPlace(updatedPerson.getBirthPlace());
        existingPerson.setCurrentAddress(updatedPerson.getCurrentAddress());
        existingPerson.setNotes(updatedPerson.getNotes());
        normalizePersonFields(existingPerson);

        Person saved = personRepository.save(existingPerson);
        auditLogService.record(AuditLogService.ACTION_PERSON_UPDATED, AuditLogService.ENTITY_PERSON, saved.getId(),
                "Updated person " + fullNameFor(saved));
        return saved;
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
    /** Either bound may be null (open-ended) -- see FamilyTreeAssembler's windowed tree view. */
    public List<Person> getPersonsByGenerationRange(Integer minGeneration, Integer maxGeneration) {
        return personRepository.findByGenerationNumberRange(minGeneration, maxGeneration);
    }
    public List<Person> searchPersons(String keyword){
        if (keyword == null || keyword.isBlank()) {
            return personRepository.findAllByOrderByGenerationNumberAscIdAsc();
        }
        String normalizedKeyword = keyword.trim();
        Integer generationNumber = parseGenerationNumber(normalizedKeyword);
        return personRepository.searchPersons(normalizedKeyword, generationNumber);
    }
    public Person getPersonById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));
    }
    public void deletePersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));
        String name = fullNameFor(person);

        // delete all relationships first
        relationshipService.deleteRelationshipsByPerson(person);

        // then delete person
        personRepository.delete(person);

        auditLogService.record(AuditLogService.ACTION_PERSON_DELETED, AuditLogService.ENTITY_PERSON, id, "Deleted person " + name);
    }

    private String fullNameFor(Person person) {
        StringBuilder name = new StringBuilder();
        if (person.getFirstName() != null) name.append(person.getFirstName());
        if (person.getLastName() != null) {
            if (!name.isEmpty()) name.append(" ");
            name.append(person.getLastName());
        }
        return name.isEmpty() ? ("#" + person.getId()) : name.toString();
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
        normalizePersonFields(person);

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
        normalizePersonFields(existingPerson);

        return personRepository.save(existingPerson);
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

    public int clearAutogeneratedNepaliNames() {
        List<Person> persons = personRepository.findAll();
        List<Person> updatedPersons = new ArrayList<>();

        for (Person person : persons) {
            boolean changed = false;

            if (matchesGeneratedNepaliValue(person.getFirstName(), person.getFirstNameNepali())) {
                person.setFirstNameNepali(null);
                changed = true;
            }
            if (matchesGeneratedNepaliValue(person.getMiddleName(), person.getMiddleNameNepali())) {
                person.setMiddleNameNepali(null);
                changed = true;
            }
            if (matchesGeneratedNepaliValue(person.getLastName(), person.getLastNameNepali())) {
                person.setLastNameNepali(null);
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

    private void normalizePersonFields(Person person) {
        person.setFirstName(normalizeToNull(person.getFirstName()));
        person.setMiddleName(normalizeToNull(person.getMiddleName()));
        person.setLastName(normalizeToNull(person.getLastName()));
        person.setFirstNameNepali(normalizeToNull(person.getFirstNameNepali()));
        person.setMiddleNameNepali(normalizeToNull(person.getMiddleNameNepali()));
        person.setLastNameNepali(normalizeToNull(person.getLastNameNepali()));
        person.setNickname(normalizeToNull(person.getNickname()));
        person.setGender(normalizeToNull(person.getGender()));
        person.setPhotoPath(normalizeToNull(person.getPhotoPath()));
        person.setBirthPlace(normalizeToNull(person.getBirthPlace()));
        person.setCurrentAddress(normalizeToNull(person.getCurrentAddress()));
        person.setNotes(normalizeToNull(person.getNotes()));
    }

    private Integer parseGenerationNumber(String keyword) {
        try {
            return Integer.valueOf(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private boolean matchesGeneratedNepaliValue(String englishValue, String nepaliValue) {
        String storedNepali = normalizeToNull(nepaliValue);
        if (storedNepali == null) {
            return false;
        }

        String normalizedEnglish = normalizeToNull(englishValue);
        if (normalizedEnglish == null) {
            return false;
        }

        String generated = nameTransliterationService.transliterate(normalizedEnglish);
        return generated != null && storedNepali.equals(generated.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeToNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

}
