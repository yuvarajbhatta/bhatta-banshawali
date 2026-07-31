package com.familytree.services;

import com.familytree.dto.FamilySnapshotDto;
import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
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
 *
 * Birth date and current address are sensitive (docs/09-security-threat-model.md,
 * the Privacy page's "contact details and full birth dates" language) --
 * every method takes a ViewerContext and redacts those two fields to
 * null unless the viewer is an admin or is looking at their own linked
 * Person. This is a fixed rule, not the full per-field PrivacyPreference
 * classification in docs/04's target schema.
 */
@Service
public class PersonProfileAssembler {

    private final RelationshipService relationshipService;
    private final PersonDisplayHelper personDisplay;

    public PersonProfileAssembler(RelationshipService relationshipService, PersonDisplayHelper personDisplay) {
        this.relationshipService = relationshipService;
        this.personDisplay = personDisplay;
    }

    public PersonSummaryDto summarize(Person person, ViewerContext viewer) {
        return summarize(person, viewer, null);
    }

    /**
     * Same as summarize(), but also resolves the person's father's name into
     * parentHint -- for search-result pickers (e.g. admin "link account to
     * person") where several same-named people need telling apart. Not used
     * for familySnapshot()'s father/mother/spouses/children entries, since
     * those would each need their own father resolved recursively for no
     * benefit (those rows already show whose father/mother they are).
     */
    public PersonSummaryDto summarizeForSearch(Person person, ViewerContext viewer) {
        Person father = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)
                .stream().findFirst().map(Relationship::getRelatedPerson).orElse(null);
        String parentHint = father == null ? null : personDisplay.englishFullName(father);
        return summarize(person, viewer, parentHint);
    }

    private PersonSummaryDto summarize(Person person, ViewerContext viewer, String parentHint) {
        boolean canSeeSensitive = viewer.canSeeSensitiveFieldsFor(person.getId());
        return new PersonSummaryDto(
                person.getId(),
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getGenerationNumber(),
                person.getGender(),
                canSeeSensitive ? person.getBirthDate() : null,
                parentHint
        );
    }

    public FamilySnapshotDto familySnapshot(Person person, ViewerContext viewer) {
        PersonSummaryDto father = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)
                .stream().findFirst().map(r -> summarize(r.getRelatedPerson(), viewer)).orElse(null);
        PersonSummaryDto mother = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER)
                .stream().findFirst().map(r -> summarize(r.getRelatedPerson(), viewer)).orElse(null);
        List<PersonSummaryDto> spouses = relationshipService.getSpousesForPerson(person).stream()
                .map(p -> summarize(p, viewer)).toList();
        List<PersonSummaryDto> children = relationshipService.getChildrenForPerson(person).stream()
                .map(p -> summarize(p, viewer)).toList();

        return new FamilySnapshotDto(father, mother, spouses, children);
    }

    public PersonDetailDto detail(Person person, ViewerContext viewer) {
        boolean canSeeSensitive = viewer.canSeeSensitiveFieldsFor(person.getId());
        return new PersonDetailDto(
                person.getId(),
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getNickname(),
                person.getGender(),
                person.getGenerationNumber(),
                canSeeSensitive ? person.getBirthDate() : null,
                person.getDeathDate(),
                person.getBirthPlace(),
                canSeeSensitive ? person.getCurrentAddress() : null,
                person.getNotes(),
                person.getPhotoPath(),
                familySnapshot(person, viewer)
        );
    }
}
