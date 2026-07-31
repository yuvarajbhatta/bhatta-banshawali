package com.familytree.services;

import com.familytree.dto.FamilyTreeDto;
import com.familytree.dto.PersonTreeNodeDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyTreeAssemblerTest {

    @Mock
    private PersonService personService;

    @Mock
    private RelationshipService relationshipService;

    private final FamilyTreeAssembler assembler() {
        return new FamilyTreeAssembler(personService, relationshipService, new PersonDisplayHelper());
    }

    private Person person(long id, String firstName, String lastName, LocalDate birthDate) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setGender("MALE");
        person.setGenerationNumber((int) id);
        person.setBirthDate(birthDate);
        return person;
    }

    private Relationship relationship(Person person, Person related, RelationshipType type) {
        Relationship relationship = new Relationship();
        relationship.setPerson(person);
        relationship.setRelatedPerson(related);
        relationship.setRelationshipType(type);
        return relationship;
    }

    @Test
    void buildsNodesWithFatherSpouseAndChildReferences() {
        Person father = person(1L, "Rana", "Bhatta", LocalDate.of(1940, 1, 1));
        Person mother = person(2L, "Sita", "Bhatta", LocalDate.of(1945, 1, 1));
        Person child = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));

        when(personService.getAllPersons()).thenReturn(List.of(father, mother, child));
        when(relationshipService.getAllRelationships()).thenReturn(List.of(
                relationship(child, father, RelationshipType.FATHER),
                relationship(child, mother, RelationshipType.MOTHER),
                relationship(father, child, RelationshipType.CHILD),
                relationship(mother, child, RelationshipType.CHILD),
                relationship(father, mother, RelationshipType.SPOUSE),
                relationship(mother, father, RelationshipType.SPOUSE)
        ));
        when(relationshipService.getRootPersonForLineage()).thenReturn(father);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(true, null));

        assertThat(tree.rootPersonId()).isEqualTo(1L);
        assertThat(tree.nodes()).hasSize(3);

        PersonTreeNodeDto childNode = findNode(tree, 3L);
        assertThat(childNode.fatherId()).isEqualTo(1L);
        assertThat(childNode.motherId()).isEqualTo(2L);
        assertThat(childNode.englishFullName()).isEqualTo("Yuva Bhatta");

        PersonTreeNodeDto fatherNode = findNode(tree, 1L);
        assertThat(fatherNode.childIds()).containsExactly(3L);
        assertThat(fatherNode.spouseIds()).containsExactly(2L);
    }

    @Test
    void redactsBirthDateForNonAdminViewingSomeoneElse() {
        Person self = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));
        when(personService.getAllPersons()).thenReturn(List.of(self));
        when(relationshipService.getAllRelationships()).thenReturn(List.of());
        when(relationshipService.getRootPersonForLineage()).thenReturn(null);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(false, 99L));

        assertThat(tree.rootPersonId()).isNull();
        assertThat(findNode(tree, 3L).birthDate()).isNull();
    }

    @Test
    void showsBirthDateForViewersOwnLinkedPerson() {
        Person self = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));
        when(personService.getAllPersons()).thenReturn(List.of(self));
        when(relationshipService.getAllRelationships()).thenReturn(List.of());
        when(relationshipService.getRootPersonForLineage()).thenReturn(null);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(false, 3L));

        assertThat(findNode(tree, 3L).birthDate()).isEqualTo(LocalDate.of(1970, 1, 1));
    }

    @Test
    void windowedBuildReturnsOnlyPersonsWithinGenerationRange() {
        Person father = person(1L, "Rana", "Bhatta", LocalDate.of(1940, 1, 1));
        Person mother = person(2L, "Sita", "Bhatta", LocalDate.of(1945, 1, 1));
        Person child = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));

        when(personService.getPersonsByGenerationRange(2, 3)).thenReturn(List.of(mother, child));
        when(relationshipService.getAllRelationships()).thenReturn(List.of(
                relationship(child, father, RelationshipType.FATHER),
                relationship(child, mother, RelationshipType.MOTHER),
                relationship(father, child, RelationshipType.CHILD),
                relationship(mother, child, RelationshipType.CHILD)
        ));
        when(relationshipService.getRootPersonForLineage()).thenReturn(father);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(true, null), 2, 3);

        assertThat(tree.nodes()).hasSize(2);
        assertThat(tree.nodes()).extracting(PersonTreeNodeDto::id).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void windowedBuildKeepsRawReferencesToPersonsOutsideTheWindow() {
        // Father (generation 1) is outside the [2,3] window, but the child
        // node still must carry his raw id -- the frontend's own
        // edge-filtering (useFamilyTreeLayout.ts) decides what to draw with it,
        // this endpoint never nulls out-of-window references itself.
        Person father = person(1L, "Rana", "Bhatta", LocalDate.of(1940, 1, 1));
        Person child = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));

        when(personService.getPersonsByGenerationRange(2, 3)).thenReturn(List.of(child));
        when(relationshipService.getAllRelationships()).thenReturn(List.of(
                relationship(child, father, RelationshipType.FATHER),
                relationship(father, child, RelationshipType.CHILD)
        ));
        when(relationshipService.getRootPersonForLineage()).thenReturn(father);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(true, null), 2, 3);

        assertThat(tree.nodes()).hasSize(1);
        assertThat(findNode(tree, 3L).fatherId()).isEqualTo(1L);
    }

    @Test
    void windowedBuildStillRedactsBirthDateForNonAdminViewingSomeoneElse() {
        Person self = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));
        when(personService.getPersonsByGenerationRange(3, 3)).thenReturn(List.of(self));
        when(relationshipService.getAllRelationships()).thenReturn(List.of());
        when(relationshipService.getRootPersonForLineage()).thenReturn(null);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(false, 99L), 3, 3);

        assertThat(findNode(tree, 3L).birthDate()).isNull();
    }

    @Test
    void unwindowedOverloadDelegatesToNullBounds() {
        Person self = person(3L, "Yuva", "Bhatta", LocalDate.of(1970, 1, 1));
        when(personService.getAllPersons()).thenReturn(List.of(self));
        when(relationshipService.getAllRelationships()).thenReturn(List.of());
        when(relationshipService.getRootPersonForLineage()).thenReturn(null);

        FamilyTreeDto tree = assembler().buildTree(new ViewerContext(true, null));

        assertThat(tree.nodes()).hasSize(1);
    }

    private PersonTreeNodeDto findNode(FamilyTreeDto tree, long id) {
        return tree.nodes().stream()
                .filter(node -> node.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No node with id " + id));
    }
}
