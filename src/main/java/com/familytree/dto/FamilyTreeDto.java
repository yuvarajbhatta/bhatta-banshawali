package com.familytree.dto;

import java.util.List;

public record FamilyTreeDto(
        List<PersonTreeNodeDto> nodes,
        Long rootPersonId
) {
}
