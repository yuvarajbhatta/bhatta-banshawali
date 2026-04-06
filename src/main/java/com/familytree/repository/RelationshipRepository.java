package com.familytree.repository;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    List<Relationship> findByPersonAndRelationshipType(Person person, RelationshipType type);
    List<Relationship> findByRelatedPersonAndRelationshipType(Person relatedPerson, RelationshipType type);
    boolean existsByPersonAndRelatedPersonAndRelationshipType(Person person,
                                                              Person relatedPerson,
                                                              RelationshipType relationshipType);

    void deleteByPersonOrRelatedPerson(Person person, Person relatedPerson);
    List<Relationship> findByPersonAndRelationshipTypeOrderByRelatedPersonIdAsc(Person person, RelationshipType relationshipType);
}
