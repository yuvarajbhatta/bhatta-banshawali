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
    void searchPersonsMatchesEnglishAndNepaliFields() {
        personRepository.save(createPerson("Yuva", "Bhatta", 2));
        Person mina = createPerson("Mina", "Sharma", 3);
        mina.setFirstNameNepali("मिना");
        personRepository.save(mina);

        List<Person> englishResults = personRepository.searchPersons("yu", null);
        List<Person> nepaliResults = personRepository.searchPersons("मि", null);

        assertThat(englishResults).extracting(Person::getFirstName).containsExactly("Yuva");
        assertThat(nepaliResults).extracting(Person::getFirstNameNepali).containsExactly("मिना");
    }

    @Test
    void searchPersonsMatchesNicknameNotesAndGeneration() {
        Person person = createPerson("Jhanka", "Bhatta", 7);
        person.setNickname("JN");
        person.setNotes("Branch migrated to Kathmandu");
        personRepository.save(person);

        assertThat(personRepository.searchPersons("jn", null)).extracting(Person::getNickname).containsExactly("JN");
        assertThat(personRepository.searchPersons("kathmandu", null)).extracting(Person::getFirstName).containsExactly("Jhanka");
        assertThat(personRepository.searchPersons("7", 7)).extracting(Person::getGenerationNumber).containsExactly(7);
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

    @Test
    void countDistinctGenerationNumbersIgnoresNullsAndDuplicates() {
        personRepository.save(createPerson("A", "Bhatta", 1));
        personRepository.save(createPerson("B", "Bhatta", 2));
        personRepository.save(createPerson("C", "Bhatta", 2));
        personRepository.save(createPerson("D", "Bhatta", null));

        assertThat(personRepository.countDistinctGenerationNumbers()).isEqualTo(2);
    }

    @Test
    void findMinGenerationNumberIgnoresNulls() {
        personRepository.save(createPerson("A", "Bhatta", 5));
        personRepository.save(createPerson("B", "Bhatta", 2));
        personRepository.save(createPerson("C", "Bhatta", null));

        assertThat(personRepository.findMinGenerationNumber()).isEqualTo(2);
    }

    @Test
    void findMinGenerationNumberReturnsNullWhenNoneSet() {
        personRepository.save(createPerson("A", "Bhatta", null));

        assertThat(personRepository.findMinGenerationNumber()).isNull();
    }

    private Person createPerson(String firstName, String lastName, Integer generation) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setGenerationNumber(generation);
        return person;
    }
}
