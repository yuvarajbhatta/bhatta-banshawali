package com.familytree.repository;

import com.familytree.entity.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:person-repo;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Transactional
class PersonRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Test
    void searchByFirstNameOrLastNameIgnoresCase() {
        personRepository.save(createPerson("Yuva", "Bhatta", 2));
        personRepository.save(createPerson("Mina", "Sharma", 3));

        List<Person> results = personRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("yu", "yu");

        assertThat(results).extracting(Person::getFirstName).containsExactly("Yuva");
    }

    @Test
    void findAllByOrderByGenerationNumberAscIdAscReturnsSortedPersons() {
        Person generationTwo = personRepository.save(createPerson("Second", "Bhatta", 2));
        Person generationOne = personRepository.save(createPerson("First", "Bhatta", 1));

        List<Person> results = personRepository.findAllByOrderByGenerationNumberAscIdAsc();

        assertThat(results).extracting(Person::getId).containsSubsequence(generationOne.getId(), generationTwo.getId());
    }

    @Test
    void findAllByOrderByIdAscReturnsSortedById() {
        Person first = personRepository.save(createPerson("First", "Bhatta", 1));
        Person second = personRepository.save(createPerson("Second", "Bhatta", 2));

        List<Person> results = personRepository.findAllByOrderByIdAsc();

        assertThat(results).extracting(Person::getId).containsSubsequence(first.getId(), second.getId());
    }

    private Person createPerson(String firstName, String lastName, Integer generation) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setGenerationNumber(generation);
        return person;
    }
}
