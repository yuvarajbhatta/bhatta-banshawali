package com.familytree.entity;

import jakarta.persistence.*;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;

@Entity
@Table(name = "relationships")
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne
    @JoinColumn(name = "related_person_id", nullable = false)
    private Person relatedPerson;

    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType;

    public Long getId() {
        return id;
    }
    public Person getPerson() {
        return person;
    }
    public void setPerson(Person person) {
        this.person = person;
    }
    public Person getRelatedPerson() {
        return relatedPerson;
    }
    public void setRelatedPerson(Person relatedPerson) {
        this.relatedPerson = relatedPerson;
    }
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }
    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }
}

