package com.familytree.dto;

import com.familytree.entity.RelationshipType;
import jakarta.validation.constraints.NotNull;

public class AdminRelationshipRequestDto {

    @NotNull(message = "Person is required.")
    private Long personId;

    @NotNull(message = "Related person is required.")
    private Long relatedPersonId;

    @NotNull(message = "Relationship type is required.")
    private RelationshipType relationshipType;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Long getRelatedPersonId() {
        return relatedPersonId;
    }

    public void setRelatedPersonId(Long relatedPersonId) {
        this.relatedPersonId = relatedPersonId;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }
}
