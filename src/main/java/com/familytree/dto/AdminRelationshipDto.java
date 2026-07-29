package com.familytree.dto;

import com.familytree.entity.RelationshipType;

public record AdminRelationshipDto(
        Long id,
        Long personId,
        String personName,
        Long relatedPersonId,
        String relatedPersonName,
        RelationshipType relationshipType
) {
}
