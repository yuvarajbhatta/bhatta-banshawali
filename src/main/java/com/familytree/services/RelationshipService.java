package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.repository.RelationshipRepository;
import com.familytree.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RelationshipService {
    private final RelationshipRepository relationshipRepository;
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;

    public RelationshipService(RelationshipRepository relationshipRepository, PersonRepository personRepository,
                               AuditLogService auditLogService) {
        this.relationshipRepository = relationshipRepository;
        this.personRepository = personRepository;
        this.auditLogService = auditLogService;
    }
    public Relationship saveRelationship(Relationship relationship) {
        return relationshipRepository.save(relationship);
    }
    public List<Relationship> getAllRelationships() {
        return relationshipRepository.findAll();
    }
    public List<Relationship> getRelationshipsByPersonAndType(Person person, RelationshipType type) {
        return relationshipRepository.findByPersonAndRelationshipType(person, type);
    }
    public List<Relationship> getRelationshipsbyRelatedPersonAndRelationshipType(Person relatedPerson, RelationshipType type) {
        return relationshipRepository.findByRelatedPersonAndRelationshipType(relatedPerson, type);
    }
    public void saveRelationshipWithAutoLinks(Person person, Person relatedPerson, RelationshipType type) {
        if (wouldCreateCycle(person, relatedPerson, type)) {
            throw new RelationshipCycleException(
                    "This relationship would make " + personLabel(person) + " their own ancestor.");
        }

        saveIfMissing(person, relatedPerson, type);
        if (type == RelationshipType.FATHER || type == RelationshipType.MOTHER) {
            saveIfMissing(relatedPerson, person, RelationshipType.CHILD);
            autoCreateSpouseBetweenParents(person, relatedPerson, type);
        }
        if (type == RelationshipType.SPOUSE){
            saveIfMissing(relatedPerson, person, RelationshipType.SPOUSE);
        }
        if (type == RelationshipType.CHILD) {
            // "relatedPerson is the CHILD of person" -- the reciprocal is
            // relatedPerson's FATHER or MOTHER being person, decided by
            // person's own gender. Without this, saving a relationship as
            // "Child" (rather than "Father"/"Mother", both selectable from
            // the same form) left a one-sided edge: the child had no
            // FATHER/MOTHER edge at all, so every father/ancestor-chain
            // lookup from the child's own side silently found nothing.
            inferParentType(person).ifPresent(parentType -> {
                saveIfMissing(relatedPerson, person, parentType);
                autoCreateSpouseBetweenParents(relatedPerson, person, parentType);
            });
        }

        // Logs only the primary relationship the caller asked for, not the
        // reciprocal/auto-linked edges above -- those are implied by it,
        // not a separate admin decision worth their own log line.
        auditLogService.record(AuditLogService.ACTION_RELATIONSHIP_CREATED, AuditLogService.ENTITY_RELATIONSHIP, null,
                "Linked " + personLabel(person) + " as " + type + " of " + personLabel(relatedPerson));
    }
    private void saveIfMissing(Person person, Person relatedPerson, RelationshipType type) {
        boolean exists = relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(
                person, relatedPerson, type);
        if (!exists) {
            Relationship relationship = new Relationship();
            relationship.setPerson(person);
            relationship.setRelatedPerson(relatedPerson);
            relationship.setRelationshipType(type);
            relationshipRepository.save(relationship);
        }
    }
    private void autoCreateSpouseBetweenParents(Person child, Person newParent, RelationshipType newParentType) {
        RelationshipType otherParentType =
                (newParentType == RelationshipType.FATHER) ? RelationshipType.MOTHER : RelationshipType.FATHER;
        List<Relationship>otherParents =
                relationshipRepository.findByPersonAndRelationshipType(child, otherParentType);
        for (Relationship rel : otherParents) {
            Person otherParent = rel.getRelatedPerson();

            if (!otherParent.getId().equals(newParent.getId())) {
                saveIfMissing(newParent, otherParent, RelationshipType.SPOUSE);
                saveIfMissing(otherParent, newParent, RelationshipType.SPOUSE);
            }
        }
    }

    public List<Person> getSpousesForPerson(Person person) {
        Map<Long, Person> spouseMap = new LinkedHashMap<>();

        for (Relationship relationship : relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.SPOUSE)) {
            spouseMap.put(relationship.getRelatedPerson().getId(), relationship.getRelatedPerson());
        }

        for (Person child : getChildrenForPerson(person)) {
            for (Relationship fatherRel : relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.FATHER)) {
                Person father = fatherRel.getRelatedPerson();
                if (!father.getId().equals(person.getId())) {
                    spouseMap.putIfAbsent(father.getId(), father);
                }
            }

            for (Relationship motherRel : relationshipRepository.findByPersonAndRelationshipType(child, RelationshipType.MOTHER)) {
                Person mother = motherRel.getRelatedPerson();
                if (!mother.getId().equals(person.getId())) {
                    spouseMap.putIfAbsent(mother.getId(), mother);
                }
            }
        }

        return new ArrayList<>(spouseMap.values());
    }

    public List<Person> getChildrenForPerson(Person person) {
        Map<Long, Person> childMap = new LinkedHashMap<>();

        for (Relationship relationship : relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.CHILD)) {
            childMap.put(relationship.getRelatedPerson().getId(), relationship.getRelatedPerson());
        }

        for (Relationship relationship : relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.FATHER)) {
            childMap.putIfAbsent(relationship.getPerson().getId(), relationship.getPerson());
        }

        for (Relationship relationship : relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.MOTHER)) {
            childMap.putIfAbsent(relationship.getPerson().getId(), relationship.getPerson());
        }

        return new ArrayList<>(childMap.values());
    }
    public boolean relationshipExists(Person person, Person relatedPerson, RelationshipType type) {
        return relationshipRepository.existsByPersonAndRelatedPersonAndRelationshipType(person, relatedPerson, type);
    }

    public void deleteRelationshipById(Long id) {
        Relationship relationship = relationshipRepository.findById(id).orElse(null);
        relationshipRepository.deleteById(id);
        if (relationship != null) {
            auditLogService.record(AuditLogService.ACTION_RELATIONSHIP_DELETED, AuditLogService.ENTITY_RELATIONSHIP, id,
                    "Removed " + relationship.getRelationshipType() + " link between " + personLabel(relationship.getPerson())
                            + " and " + personLabel(relationship.getRelatedPerson()));
        }
    }
    public void deleteRelationshipsByPerson(Person person) {
        relationshipRepository.deleteByPersonOrRelatedPerson(person, person);
    }

    public Relationship getRelationshipById(Long id) {
        return relationshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relationship with id " + id + " not found"));
    }
    public Relationship updateRelationship(Long id, Person person, Person relatedPerson, RelationshipType type) {
        if (wouldCreateCycle(person, relatedPerson, type, id)) {
            throw new RelationshipCycleException(
                    "This relationship would make " + personLabel(person) + " their own ancestor.");
        }

        Relationship existingRelationship = relationshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relationship with id " + id + " not found"));
        existingRelationship.setPerson(person);
        existingRelationship.setRelatedPerson(relatedPerson);
        existingRelationship.setRelationshipType(type);

        Relationship saved = relationshipRepository.save(existingRelationship);
        auditLogService.record(AuditLogService.ACTION_RELATIONSHIP_UPDATED, AuditLogService.ENTITY_RELATIONSHIP, id,
                "Updated relationship: " + personLabel(person) + " as " + type + " of " + personLabel(relatedPerson));
        return saved;
    }
    public List<Person> getDirectChildren(Person person) {
        return relationshipRepository
                .findByPersonAndRelationshipTypeOrderByRelatedPersonIdAsc(person, RelationshipType.CHILD)
                .stream()
                .map(Relationship::getRelatedPerson)
                .toList();
    }
    public Person getRootPersonForLineage(){
        return relationshipRepository.findAll().stream()
                .filter(rel -> rel.getRelationshipType() == RelationshipType.CHILD)
                .map(Relationship::getPerson)
                .filter(person -> relationshipRepository
                        .findByRelatedPersonAndRelationshipType(person, RelationshipType.CHILD)
                        .isEmpty())
                .findFirst()
                .orElse(null);
    }
    public Map<String, Object> buildLineageTree(Person rootPerson){
        return buildLineageTree(rootPerson, Locale.ENGLISH);
    }

    public Map<String, Object> buildLineageTree(Person rootPerson, Locale locale){
        if (rootPerson == null){
            return  null;
        }
        Map<Long, List<Relationship>> childrenByPersonId = relationshipRepository
                .findByRelationshipTypeOrderByPersonIdAscRelatedPersonIdAsc(RelationshipType.CHILD)
                .stream()
                .collect(Collectors.groupingBy(
                        relationship -> relationship.getPerson().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, Person> personMap = personRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Person::getId,
                        person -> person,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        return buildLineageTree(rootPerson, childrenByPersonId, personMap, locale);
    }

    private Map<String, Object> buildLineageTree(Person person,
                                                 Map<Long, List<Relationship>> childrenByPersonId) {
        Map<Long, Person> personMap = personRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Person::getId,
                        existingPerson -> existingPerson,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        return buildLineageTree(person, childrenByPersonId, personMap, Locale.ENGLISH);
    }

    private Map<String, Object> buildLineageTree(Person person,
                                                 Map<Long, List<Relationship>> childrenByPersonId,
                                                 Map<Long, Person> personMap,
                                                 Locale locale) {
        if (person == null) {
            return null;
        }

        Map<String, Object> node = new LinkedHashMap<>();
        boolean nepali = locale != null && "ne".equalsIgnoreCase(locale.getLanguage());
        String englishName = buildPersonName(person, false);
        String nepaliName = buildPersonName(person, true);
        String fullName = nepali && !nepaliName.isBlank() ? nepaliName : englishName;

        node.put("id", person.getId());
        node.put("dbId", person.getId());
        node.put("parentDbId", null);
        node.put("generationNumber", person.getGenerationNumber());
        node.put("name", fullName);
        node.put("englishName", englishName);
        node.put("nepaliName", nepaliName);
        node.put("photoPath", person.getPhotoPath());

        List<Map<String, Object>> children = new ArrayList<>();
        List<Relationship> childRelationships = childrenByPersonId.getOrDefault(person.getId(), Collections.emptyList());
        for (Relationship relationship : childRelationships) {
            Person child = personMap.get(relationship.getRelatedPerson().getId());
            Map<String, Object> childNode = buildLineageTree(child, childrenByPersonId, personMap, locale);
            if (childNode != null) {
                childNode.put("parentDbId", person.getId());
                children.add(childNode);
            }
        }
        node.put("children", children);
        return node;
    }

    private String buildPersonName(Person person, boolean nepali) {
        List<String> parts = new ArrayList<>();
        if (nepali) {
            addIfPresent(parts, person.getFirstNameNepali());
            addIfPresent(parts, person.getMiddleNameNepali());
            addIfPresent(parts, person.getLastNameNepali());
            return String.join(" ", parts);
        }

        addIfPresent(parts, person.getFirstName());
        addIfPresent(parts, person.getMiddleName());
        addIfPresent(parts, person.getLastName());
        return String.join(" ", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    /**
     * Returns true if saving (person, relatedPerson, type) would make some
     * person their own ancestor -- either directly (person == relatedPerson)
     * or transitively through an existing parent/child chain.
     */
    public boolean wouldCreateCycle(Person person, Person relatedPerson, RelationshipType type) {
        return wouldCreateCycle(person, relatedPerson, type, null);
    }

    /**
     * Same as {@link #wouldCreateCycle(Person, Person, RelationshipType)},
     * but ignores the relationship row identified by excludeRelationshipId
     * when walking existing parent/child links -- used when checking an
     * update, since the row being replaced should not count as evidence
     * against itself.
     */
    public boolean wouldCreateCycle(Person person, Person relatedPerson, RelationshipType type, Long excludeRelationshipId) {
        if (person == null || relatedPerson == null || type == null) {
            return false;
        }
        if (person.getId() != null && person.getId().equals(relatedPerson.getId())) {
            return true;
        }

        Person parentCandidate;
        Person childCandidate;
        switch (type) {
            case FATHER, MOTHER -> {
                parentCandidate = relatedPerson;
                childCandidate = person;
            }
            case CHILD -> {
                parentCandidate = person;
                childCandidate = relatedPerson;
            }
            default -> {
                return false;
            }
        }

        return isAncestor(childCandidate, parentCandidate, excludeRelationshipId);
    }

    /**
     * Walks up the parent chain from descendant, returning true if
     * candidateAncestor is found along the way. Guards against pre-existing
     * cycles in the data with a visited-set so it cannot loop forever.
     */
    private boolean isAncestor(Person candidateAncestor, Person descendant, Long excludeRelationshipId) {
        Set<Long> visited = new HashSet<>();
        Deque<Person> toVisit = new ArrayDeque<>();
        toVisit.push(descendant);

        while (!toVisit.isEmpty()) {
            Person current = toVisit.pop();
            if (current.getId() == null || !visited.add(current.getId())) {
                continue;
            }

            for (Person parent : getParentsForPerson(current, excludeRelationshipId)) {
                if (parent.getId() != null && parent.getId().equals(candidateAncestor.getId())) {
                    return true;
                }
                toVisit.push(parent);
            }
        }

        return false;
    }

    public List<Person> getParentsForPerson(Person person) {
        return getParentsForPerson(person, null);
    }

    /**
     * Resolves person's father, falling back to a reversed CHILD-type edge
     * (person as relatedPerson, i.e. "the father saved this as -- I have a
     * child -- instead of -- this is my father") when no direct FATHER edge
     * exists. This historical shape can happen because saveRelationshipWithAutoLinks
     * only auto-completes the reciprocal for FATHER/MOTHER/SPOUSE saves, not
     * for a CHILD-type save made directly -- an admin picking "Child" from
     * the Relationships form's type dropdown produces exactly this one-sided
     * edge. Falls back to the candidate's gender to tell a reversed father
     * edge apart from a reversed mother edge recorded the same way.
     */
    public Optional<Person> getFatherForPerson(Person person) {
        return getParentByTypeWithFallback(person, RelationshipType.FATHER, "m");
    }

    /** Same fallback as getFatherForPerson, for the mother line. */
    public Optional<Person> getMotherForPerson(Person person) {
        return getParentByTypeWithFallback(person, RelationshipType.MOTHER, "f");
    }

    /** Other children of either parent -- see PersonPhotoService's upload-authorization check. */
    public List<Person> getSiblingsForPerson(Person person) {
        Map<Long, Person> siblingMap = new LinkedHashMap<>();

        getFatherForPerson(person).ifPresent(father -> getChildrenForPerson(father).forEach(child -> {
            if (!child.getId().equals(person.getId())) {
                siblingMap.put(child.getId(), child);
            }
        }));
        getMotherForPerson(person).ifPresent(mother -> getChildrenForPerson(mother).forEach(child -> {
            if (!child.getId().equals(person.getId())) {
                siblingMap.put(child.getId(), child);
            }
        }));

        return new ArrayList<>(siblingMap.values());
    }

    private Optional<Person> getParentByTypeWithFallback(Person person, RelationshipType type, String genderPrefix) {
        Optional<Person> direct = relationshipRepository.findByPersonAndRelationshipType(person, type)
                .stream().findFirst().map(Relationship::getRelatedPerson);
        if (direct.isPresent()) {
            return direct;
        }
        return relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.CHILD).stream()
                .map(Relationship::getPerson)
                .filter(candidate -> matchesGenderPrefix(candidate, genderPrefix))
                .findFirst();
    }

    /** FATHER if person reads as male, MOTHER if female, empty if the gender field can't tell us -- see saveRelationshipWithAutoLinks's CHILD-type branch. */
    private Optional<RelationshipType> inferParentType(Person person) {
        if (matchesGenderPrefix(person, "m")) {
            return Optional.of(RelationshipType.FATHER);
        }
        if (matchesGenderPrefix(person, "f")) {
            return Optional.of(RelationshipType.MOTHER);
        }
        return Optional.empty();
    }

    private boolean matchesGenderPrefix(Person person, String genderPrefix) {
        return person.getGender() != null && person.getGender().toLowerCase(Locale.ROOT).startsWith(genderPrefix);
    }

    private List<Person> getParentsForPerson(Person person, Long excludeRelationshipId) {
        Map<Long, Person> parents = new LinkedHashMap<>();

        for (Relationship relationship : relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.FATHER)) {
            if (!isExcluded(relationship, excludeRelationshipId)) {
                parents.put(relationship.getRelatedPerson().getId(), relationship.getRelatedPerson());
            }
        }
        for (Relationship relationship : relationshipRepository.findByPersonAndRelationshipType(person, RelationshipType.MOTHER)) {
            if (!isExcluded(relationship, excludeRelationshipId)) {
                parents.put(relationship.getRelatedPerson().getId(), relationship.getRelatedPerson());
            }
        }
        for (Relationship relationship : relationshipRepository.findByRelatedPersonAndRelationshipType(person, RelationshipType.CHILD)) {
            if (!isExcluded(relationship, excludeRelationshipId)) {
                parents.put(relationship.getPerson().getId(), relationship.getPerson());
            }
        }

        return new ArrayList<>(parents.values());
    }

    private boolean isExcluded(Relationship relationship, Long excludeRelationshipId) {
        return excludeRelationshipId != null && excludeRelationshipId.equals(relationship.getId());
    }

    private String personLabel(Person person) {
        String name = buildPersonName(person, false);
        return name.isBlank() ? "this person" : name;
    }
}
