package com.familytree.services;

import com.familytree.dto.FamilySnapshotDto;
import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.entity.RelationshipType;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds the member-facing Person DTOs (docs/08 Phase 4: dashboard,
 * person detail pages) from a Person entity -- shared by
 * MemberProfileController (GET /api/v1/me) and PersonApiController
 * (GET /api/v1/persons/{id}) so the family-snapshot logic exists in
 * exactly one place.
 */
@Service
public class PersonProfileAssembler {

    private final RelationshipService relationshipService;
    private final PersonDisplayHelper personDisplay;

    public PersonProfileAssembler(RelationshipService relationshipService, PersonDisplayHelper personDisplay) {
        this.relationshipService = relationshipService;
        this.personDisplay = personDisplay;
    }

    public PersonSummaryDto summarize(Person person) {
        return new PersonSummaryDto(
                person.getId(),
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getGenerationNumber(),
                person.getGender(),
                person.getBirthDate()
        );
    }

    public FamilySnapshotDto familySnapshot(Person person) {
        PersonSummaryDto father = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)
                .stream().findFirst().map(r -> summarize(r.getRelatedPerson())).orElse(null);
        PersonSummaryDto mother = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER)
                .stream().findFirst().map(r -> summarize(r.getRelatedPerson())).orElse(null);
        List<PersonSummaryDto> spouses = relationshipService.getSpousesForPerson(person).stream()
                .map(this::summarize).toList();
        List<PersonSummaryDto> children = relationshipService.getChildrenForPerson(person).stream()
                .map(this::summarize).toList();

        return new FamilySnapshotDto(father, mother, spouses, children);
    }

    public PersonDetailDto detail(Person person) {
        return new PersonDetailDto(
                person.getId(),
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getNickname(),
                person.getGender(),
                person.getGenerationNumber(),
                person.getBirthDate(),
                person.getDeathDate(),
                person.getBirthPlace(),
                person.getCurrentAddress(),
                person.getNotes(),
                person.getPhotoPath(),
                familySnapshot(person)
        );
    }
}
