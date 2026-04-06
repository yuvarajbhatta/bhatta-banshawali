package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final RelationshipService relationshipService;

    public PersonService(PersonRepository personRepository, RelationshipService relationshipService) {
        this.personRepository = personRepository;
        this.relationshipService = relationshipService;
    }

    public Person savePerson(Person person) {
        return personRepository.save(person);
    }

    public Person updatePerson(Long id, Person updatedPerson) {
        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));

        existingPerson.setGenerationNumber(updatedPerson.getGenerationNumber());
        existingPerson.setFirstName(updatedPerson.getFirstName());
        existingPerson.setMiddleName(updatedPerson.getMiddleName());
        existingPerson.setLastName(updatedPerson.getLastName());
        existingPerson.setGender(updatedPerson.getGender());
        existingPerson.setBirthDate(updatedPerson.getBirthDate());
        existingPerson.setDeathDate(updatedPerson.getDeathDate());
        existingPerson.setNotes(updatedPerson.getNotes());

        return personRepository.save(existingPerson);
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
    public List<Person> searchPersons(String keyword){
        if (keyword == null || keyword.isBlank()) {
            return personRepository.findAll();
        }
        return personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(keyword, keyword);
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
        person.setLastName("Bhatta");
        person.setGender("Male");
        person.setGenerationNumber(generationNumber);

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
        existingPerson.setLastName("Bhatta");
        existingPerson.setGender("Male");
        existingPerson.setGenerationNumber(generationNumber);

        return personRepository.save(existingPerson);
    }

}
