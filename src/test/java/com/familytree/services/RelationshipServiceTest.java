package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private RelationshipService relationshipService;

    @Test
    void saveRelationshipDelegatesToRepository() {
        Relationship relationship = new Relationship();
        when(relationshipRepository.save(relationship)).thenReturn(relationship);

        assertThat(relationshipService.saveRelationship(relationship)).isEqualTo(relationship);
    }

    @Test
    void getAllRelationshipsDelegatesToRepository() {
        List<Relationship> relationships = List.of(new Relationship());
        when(relationshipRepository.findAll()).thenReturn(relationships);

        assertThat(relationshipService.getAllRelationships()).isEqualTo(relationships);
    }

    @Test
    void getRelationshipsByPersonAndTypeDelegatesToRepository() {
        Person person = createPerson(1L, "Yuva");
        List<Relationship> relationships = List.of(new Relationship());
        when(relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.FATHER)).thenReturn(relationships);

        assertThat(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)).isEqualTo(relationships);
    }

    @Test
    void saveRelationshipWithAutoLinksCreatesReverseChildLinkForParentRelationship() {
        Person child = createPerson(1L, "Child");
        Person father = createPerson(2L, "Father");
        when(relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(any(), any(), any())).thenReturn(false);
        when(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.MOTHER)).thenReturn(List.of());
        noKnownParentsFor(father);

        relationshipService.saveRelationshipWithAutoLinks(child, father, RelationshipType.FATHER);

        ArgumentCaptor<Relationship> captor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Relationship::getRelationshipType)
                .containsExactlyInAnyOrder(RelationshipType.FATHER, RelationshipType.CHILD);
    }

    @Test
    void saveRelationshipWithAutoLinksCreatesReverseSpouseLink() {
        Person person = createPerson(1L, "Yuva");
        Person spouse = createPerson(2L, "Mina");
        when(relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(any(), any(), any())).thenReturn(false);

        relationshipService.saveRelationshipWithAutoLinks(person, spouse, RelationshipType.SPOUSE);

        ArgumentCaptor<Relationship> captor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
    }

    @Test
    void saveRelationshipWithAutoLinksCreatesSpouseBetweenParentsWhenOtherParentExists() {
        Person child = createPerson(1L, "Child");
        Person mother = createPerson(2L, "Mother");
        Person father = createPerson(3L, "Father");
        Relationship existingMotherRelationship = new Relationship();
        existingMotherRelationship.setRelatedPerson(mother);

        when(relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(any(), any(), any())).thenReturn(false);
        when(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.MOTHER))
                .thenReturn(List.of(existingMotherRelationship));
        noKnownParentsFor(father);

        relationshipService.saveRelationshipWithAutoLinks(child, father, RelationshipType.FATHER);

        ArgumentCaptor<Relationship> captor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Relationship::getRelationshipType)
                .contains(RelationshipType.SPOUSE);
    }

    @Test
    void getSpousesForPersonCombinesDirectAndInferredSpouses() {
        Person person = createPerson(1L, "Yuva");
        Person spouse = createPerson(2L, "Mina");
        Person child = createPerson(3L, "Child");
        Relationship spouseRelationship = new Relationship();
        spouseRelationship.setRelatedPerson(spouse);
        Relationship motherRelationship = new Relationship();
        motherRelationship.setRelatedPerson(spouse);

        when(relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.SPOUSE))
                .thenReturn(List.of(spouseRelationship));
        when(relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.CHILD))
                .thenReturn(List.of(relationship(person, child, RelationshipType.CHILD)));
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.FATHER))
                .thenReturn(List.of());
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.MOTHER))
                .thenReturn(List.of());
        when(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.FATHER))
                .thenReturn(List.of());
        when(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.MOTHER))
                .thenReturn(List.of(motherRelationship));

        List<Person> spouses = relationshipService.getSpousesForPerson(person);

        assertThat(spouses).containsExactly(spouse);
    }

    @Test
    void getChildrenForPersonCombinesDirectAndInverseLinks() {
        Person parent = createPerson(1L, "Parent");
        Person child = createPerson(2L, "Child");

        when(relationshipRepository.findByPersonAndRelationshipType(parent, RelationshipType.CHILD))
                .thenReturn(List.of(relationship(parent, child, RelationshipType.CHILD)));
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(parent, RelationshipType.FATHER))
                .thenReturn(List.of());
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(parent, RelationshipType.MOTHER))
                .thenReturn(List.of());

        List<Person> children = relationshipService.getChildrenForPerson(parent);

        assertThat(children).containsExactly(child);
    }

    @Test
    void relationshipExistsDelegatesToRepository() {
        Person person = createPerson(1L, "Yuva");
        Person relatedPerson = createPerson(2L, "Bhoj");
        when(relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(person, relatedPerson, RelationshipType.FATHER))
                .thenReturn(true);

        assertThat(relationshipService.relationshipExists(person, relatedPerson, RelationshipType.FATHER)).isTrue();
    }

    @Test
    void deleteRelationshipByIdDelegatesToRepository() {
        relationshipService.deleteRelationshipById(5L);

        verify(relationshipRepository).deleteById(5L);
    }

    @Test
    void deleteRelationshipsByPersonDelegatesToRepository() {
        Person person = createPerson(1L, "Yuva");

        relationshipService.deleteRelationshipsByPerson(person);

        verify(relationshipRepository).deleteByPersonOrRelatedPerson(person, person);
    }

    @Test
    void getRelationshipByIdReturnsRelationship() {
        Relationship relationship = new Relationship();
        when(relationshipRepository.findById(5L)).thenReturn(Optional.of(relationship));

        assertThat(relationshipService.getRelationshipById(5L)).isEqualTo(relationship);
    }

    @Test
    void getRelationshipByIdThrowsWhenMissing() {
        when(relationshipRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> relationshipService.getRelationshipById(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Relationship with id 5 not found");
    }

    @Test
    void updateRelationshipUpdatesFieldsAndSaves() {
        Relationship relationship = new Relationship();
        Person person = createPerson(1L, "Yuva");
        Person relatedPerson = createPerson(2L, "Bhoj");
        when(relationshipRepository.findById(5L)).thenReturn(Optional.of(relationship));
        when(relationshipRepository.save(relationship)).thenReturn(relationship);

        Relationship updated = relationshipService.updateRelationship(5L, person, relatedPerson, RelationshipType.FATHER);

        assertThat(updated.getPerson()).isEqualTo(person);
        assertThat(updated.getRelatedPerson()).isEqualTo(relatedPerson);
        assertThat(updated.getRelationshipType()).isEqualTo(RelationshipType.FATHER);
    }

    @Test
    void getDirectChildrenMapsRelationshipTargets() {
        Person parent = createPerson(1L, "Parent");
        Person child = createPerson(2L, "Child");
        when(relationshipRepository.findByPersonAndRelationshipTypeOrderByRelatedPersonIdAsc(parent, RelationshipType.CHILD))
                .thenReturn(List.of(relationship(parent, child, RelationshipType.CHILD)));

        assertThat(relationshipService.getDirectChildren(parent)).containsExactly(child);
    }

    @Test
    void getParentsForPersonCombinesFatherAndMother() {
        Person child = createPerson(1L, "Child");
        Person father = createPerson(2L, "Father");
        Person mother = createPerson(3L, "Mother");
        when(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.FATHER))
                .thenReturn(List.of(relationship(child, father, RelationshipType.FATHER)));
        when(relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.MOTHER))
                .thenReturn(List.of(relationship(child, mother, RelationshipType.MOTHER)));
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(child, RelationshipType.CHILD))
                .thenReturn(List.of());

        assertThat(relationshipService.getParentsForPerson(child)).containsExactlyInAnyOrder(father, mother);
    }

    @Test
    void getRootPersonForLineageReturnsPersonWithoutParentChildLink() {
        Person root = createPerson(1L, "Root");
        Person child = createPerson(2L, "Child");
        when(relationshipRepository.findAll()).thenReturn(List.of(relationship(root, child, RelationshipType.CHILD)));
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(root, RelationshipType.CHILD)).thenReturn(List.of());

        assertThat(relationshipService.getRootPersonForLineage()).isEqualTo(root);
    }

    @Test
    void buildLineageTreeBuildsNestedNodeMap() {
        Person root = createPerson(1L, "Root");
        root.setMiddleName("Ancestor");
        root.setGenerationNumber(1);
        Person child = createPerson(2L, "Child");
        child.setGenerationNumber(2);

        when(relationshipRepository.findByRelationshipTypeOrderByPersonIdAscRelatedPersonIdAsc(RelationshipType.CHILD))
                .thenReturn(List.of(relationship(root, child, RelationshipType.CHILD)));
        when(personRepository.findAll()).thenReturn(List.of(root, child));

        Map<String, Object> tree = relationshipService.buildLineageTree(root);

        assertThat(tree.get("dbId")).isEqualTo(1L);
        assertThat(tree.get("name")).isEqualTo("Root Ancestor Bhatta");
        assertThat(tree.get("englishName")).isEqualTo("Root Ancestor Bhatta");
        assertThat(tree.get("nepaliName")).isEqualTo("");
        assertThat((List<?>) tree.get("children")).hasSize(1);
    }

    @Test
    void buildLineageTreeReturnsNullWhenRootIsNull() {
        assertThat(relationshipService.buildLineageTree(null)).isNull();
    }

    @Test
    void wouldCreateCycleIsTrueForSelfRelationship() {
        Person person = createPerson(1L, "Yuva");

        assertThat(relationshipService.wouldCreateCycle(person, person, RelationshipType.FATHER)).isTrue();
    }

    @Test
    void wouldCreateCycleIsFalseForSpouseRegardlessOfAncestry() {
        Person person = createPerson(1L, "Yuva");
        Person spouse = createPerson(2L, "Mina");

        assertThat(relationshipService.wouldCreateCycle(person, spouse, RelationshipType.SPOUSE)).isFalse();
    }

    @Test
    void wouldCreateCycleIsFalseWhenNoExistingAncestryLink() {
        Person child = createPerson(1L, "Child");
        Person father = createPerson(2L, "Father");
        noKnownParentsFor(father);

        assertThat(relationshipService.wouldCreateCycle(child, father, RelationshipType.FATHER)).isFalse();
    }

    @Test
    void wouldCreateCycleDetectsTransitiveAncestryThroughFatherChain() {
        // Existing data: A is father of C, C is father of B (A -> C -> B).
        // Proposing "B is father of A" would close the loop A -> C -> B -> A.
        Person a = createPerson(1L, "A");
        Person b = createPerson(2L, "B");
        Person c = createPerson(3L, "C");

        when(relationshipRepository.findByPersonAndRelationshipType(b, RelationshipType.FATHER))
                .thenReturn(List.of(relationship(b, c, RelationshipType.FATHER)));
        when(relationshipRepository.findByPersonAndRelationshipType(b, RelationshipType.MOTHER)).thenReturn(List.of());
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(b, RelationshipType.CHILD)).thenReturn(List.of());
        when(relationshipRepository.findByPersonAndRelationshipType(c, RelationshipType.FATHER))
                .thenReturn(List.of(relationship(c, a, RelationshipType.FATHER)));
        when(relationshipRepository.findByPersonAndRelationshipType(c, RelationshipType.MOTHER)).thenReturn(List.of());
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(c, RelationshipType.CHILD)).thenReturn(List.of());

        assertThat(relationshipService.wouldCreateCycle(a, b, RelationshipType.FATHER)).isTrue();
    }

    @Test
    void wouldCreateCycleIgnoresExcludedRelationshipWhenEditing() {
        // The only path from B back to A is the very relationship being edited (id 99),
        // so once excluded, editing it back to itself should not be flagged as a cycle.
        Person a = createPerson(1L, "A");
        Person b = createPerson(2L, "B");
        Relationship editedRelationship = relationship(b, a, RelationshipType.FATHER);
        editedRelationship.setId(99L);

        when(relationshipRepository.findByPersonAndRelationshipType(b, RelationshipType.FATHER))
                .thenReturn(List.of(editedRelationship));
        when(relationshipRepository.findByPersonAndRelationshipType(b, RelationshipType.MOTHER)).thenReturn(List.of());
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(b, RelationshipType.CHILD)).thenReturn(List.of());

        assertThat(relationshipService.wouldCreateCycle(a, b, RelationshipType.FATHER, 99L)).isFalse();
    }

    @Test
    void saveRelationshipWithAutoLinksThrowsWhenCycleDetected() {
        Person person = createPerson(1L, "Yuva");

        assertThatThrownBy(() -> relationshipService.saveRelationshipWithAutoLinks(person, person, RelationshipType.FATHER))
                .isInstanceOf(RelationshipCycleException.class);
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void updateRelationshipThrowsWhenCycleDetected() {
        Person person = createPerson(1L, "Yuva");

        assertThatThrownBy(() -> relationshipService.updateRelationship(5L, person, person, RelationshipType.FATHER))
                .isInstanceOf(RelationshipCycleException.class);
        verify(relationshipRepository, never()).findById(any());
    }

    private void noKnownParentsFor(Person person) {
        when(relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.FATHER)).thenReturn(List.of());
        when(relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.MOTHER)).thenReturn(List.of());
        when(relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.CHILD)).thenReturn(List.of());
    }

    private Person createPerson(Long id, String firstName) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }

    private Relationship relationship(Person person, Person relatedPerson, RelationshipType type) {
        Relationship relationship = new Relationship();
        relationship.setPerson(person);
        relationship.setRelatedPerson(relatedPerson);
        relationship.setRelationshipType(type);
        return relationship;
    }
}
