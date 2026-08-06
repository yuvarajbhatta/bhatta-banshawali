package com.familytree.entity;

import jakarta.persistence.*;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;

@Entity
@Table(name = "relationships", uniqueConstraints = @UniqueConstraint(
        name = "uk_relationships_person_related_type",
        columnNames = {"person_id", "related_person_id", "relationship_type"}
))
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
    @Column(nullable = false)
    private RelationshipType relationshipType;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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

