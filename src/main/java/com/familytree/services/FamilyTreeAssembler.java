package com.familytree.services;

import com.familytree.dto.FamilyTreeDto;
import com.familytree.dto.PersonTreeNodeDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the whole-family graph for the Next.js tree view (docs/08
 * Phase 5, GET /api/v1/family-tree) out of the existing Person/
 * Relationship data -- no schema change. Mirrors the bulk-loading
 * pattern RelationshipService#buildLineageTree already uses (load
 * everything once, group into maps) rather than querying per person,
 * since this endpoint returns every person in one response.
 *
 * Reuses the same sensitive-field redaction rule as
 * PersonProfileAssembler (birth date hidden unless the viewer is an
 * admin or looking at their own linked person) so the tree can't be
 * used to see data a person's own profile page would hide.
 */
@Service
public class FamilyTreeAssembler {

    private final PersonService personService;
    private final RelationshipService relationshipService;
    private final PersonDisplayHelper personDisplay;

    public FamilyTreeAssembler(PersonService personService, RelationshipService relationshipService,
                                PersonDisplayHelper personDisplay) {
        this.personService = personService;
        this.relationshipService = relationshipService;
        this.personDisplay = personDisplay;
    }

    public FamilyTreeDto buildTree(ViewerContext viewer) {
        return buildTree(viewer, null, null);
    }

    /**
     * Windowed variant (docs/08 Phase 5 performance-benchmark follow-up) -- either bound may be
     * null (open-ended), both null returns everyone (the original behavior, still used by /family's
     * unwindowed fetch). Relationships are always loaded in full regardless of the window, never
     * filtered by it: a windowed person's child one generation below maxGeneration must still
     * appear as a raw childIds entry so the frontend's own edge-filtering (see
     * useFamilyTreeLayout.ts) can decide what to draw -- this endpoint doesn't null out
     * out-of-window references itself.
     */
    public FamilyTreeDto buildTree(ViewerContext viewer, Integer minGeneration, Integer maxGeneration) {
        List<Person> persons = (minGeneration != null || maxGeneration != null)
                ? personService.getPersonsByGenerationRange(minGeneration, maxGeneration)
                : personService.getAllPersons();
        List<Relationship> relationships = relationshipService.getAllRelationships();

        Map<Long, Long> fatherOf = new HashMap<>();
        Map<Long, Long> motherOf = new HashMap<>();
        Map<Long, List<Long>> spousesOf = new HashMap<>();
        Map<Long, List<Long>> childrenOf = new HashMap<>();

        for (Relationship relationship : relationships) {
            Long personId = relationship.getPerson().getId();
            Long relatedId = relationship.getRelatedPerson().getId();

            switch (relationship.getRelationshipType()) {
                case FATHER -> fatherOf.put(personId, relatedId);
                case MOTHER -> motherOf.put(personId, relatedId);
                case CHILD -> childrenOf.computeIfAbsent(personId, key -> new ArrayList<>()).add(relatedId);
                case SPOUSE -> spousesOf.computeIfAbsent(personId, key -> new ArrayList<>()).add(relatedId);
            }
        }

        List<PersonTreeNodeDto> nodes = persons.stream()
                .map(person -> toNode(person, viewer, fatherOf, motherOf, spousesOf, childrenOf))
                .toList();

        Person rootPerson = relationshipService.getRootPersonForLineage();
        Long rootPersonId = rootPerson != null ? rootPerson.getId() : null;

        return new FamilyTreeDto(nodes, rootPersonId);
    }

    private PersonTreeNodeDto toNode(Person person, ViewerContext viewer,
                                      Map<Long, Long> fatherOf, Map<Long, Long> motherOf,
                                      Map<Long, List<Long>> spousesOf, Map<Long, List<Long>> childrenOf) {
        boolean canSeeSensitive = viewer.canSeeSensitiveFieldsFor(person.getId());
        Long id = person.getId();

        return new PersonTreeNodeDto(
                id,
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getGender(),
                person.getGenerationNumber(),
                canSeeSensitive ? person.getBirthDate() : null,
                person.getDeathDate(),
                fatherOf.get(id),
                motherOf.get(id),
                spousesOf.getOrDefault(id, List.of()),
                childrenOf.getOrDefault(id, List.of())
        );
    }
}
