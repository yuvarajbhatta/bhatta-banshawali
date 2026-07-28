package com.familytree.dto;

import java.util.List;

public record FamilySnapshotDto(
        PersonSummaryDto father,
        PersonSummaryDto mother,
        List<PersonSummaryDto> spouses,
        List<PersonSummaryDto> children
) {
}
