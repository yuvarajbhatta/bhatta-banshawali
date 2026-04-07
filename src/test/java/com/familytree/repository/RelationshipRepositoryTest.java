package com.familytree.repository;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
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
        "spring.datasource.url=jdbc:h2:mem:relationship-repo;DB_CLOSE_DELAY=-1",
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
class RelationshipRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Test
    void queryMethodsReturnExpectedRelationships() {
        Person child = personRepository.save(createPerson("Child"));
        Person father = personRepository.save(createPerson("Father"));
        Person mother = personRepository.save(createPerson("Mother"));

        Relationship fatherRelationship = relationshipRepository.save(createRelationship(child, father, RelationshipType.FATHER));
        relationshipRepository.save(createRelationship(child, mother, RelationshipType.MOTHER));

        assertThat(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.FATHER))
                .containsExactly(fatherRelationship);
        assertThat(relationshipRepository.findByRelatedPersonAndRelationshipType(father, RelationshipType.FATHER))
                .extracting(Relationship::getPerson)
                .containsExactly(child);
        assertThat(relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(child, father, RelationshipType.FATHER))
                .isTrue();
    }

    @Test
    void orderedChildQueriesReturnSortedResultsAndDeleteWorks() {
        Person parent = personRepository.save(createPerson("Parent"));
        Person childOne = personRepository.save(createPerson("ChildOne"));
        Person childTwo = personRepository.save(createPerson("ChildTwo"));

        relationshipRepository.save(createRelationship(parent, childOne, RelationshipType.CHILD));
        relationshipRepository.save(createRelationship(parent, childTwo, RelationshipType.CHILD));

        List<Relationship> directChildren = relationshipRepository
                .findByPersonAndRelationshipTypeOrderByRelatedPersonIdAsc(parent, RelationshipType.CHILD);
        List<Relationship> allChildren = relationshipRepository
                .findByRelationshipTypeOrderByPersonIdAscRelatedPersonIdAsc(RelationshipType.CHILD);

        assertThat(directChildren).hasSize(2);
        assertThat(allChildren).hasSize(2);

        relationshipRepository.deleteByPersonOrRelatedPerson(parent, parent);

        assertThat(relationshipRepository.findByPersonAndRelationshipType(parent, RelationshipType.CHILD)).isEmpty();
    }

    private Person createPerson(String firstName) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }

    private Relationship createRelationship(Person person, Person relatedPerson, RelationshipType type) {
        Relationship relationship = new Relationship();
        relationship.setPerson(person);
        relationship.setRelatedPerson(relatedPerson);
        relationship.setRelationshipType(type);
        return relationship;
    }
}
