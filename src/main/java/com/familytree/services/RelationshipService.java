package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.repository.RelationshipRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelationshipService {
    private final RelationshipRepository relationshipRepository;

    public RelationshipService(RelationshipRepository relationshipRepository) {
        this.relationshipRepository = relationshipRepository;
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
        saveIfMissing(person, relatedPerson, type);
        if (type == RelationshipType.FATHER || type == RelationshipType.MOTHER) {
            saveIfMissing(relatedPerson, person, RelationshipType.CHILD);
            autoCreateSpouseBetweenParents(person, relatedPerson, type);
        }
        if (type == RelationshipType.SPOUSE){
            saveIfMissing(relatedPerson, person, RelationshipType.SPOUSE);
        }
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
        relationshipRepository.deleteById(id);
    }
    public void deleteRelationshipsByPerson(Person person) {
        relationshipRepository.deleteByPersonOrRelatedPerson(person, person);
    }

    public Relationship getRelationshipById(Long id) {
        return relationshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relationship with id " + id + " not found"));
    }
    public Relationship updateRelationship(Long id, Person person, Person relatedPerson, RelationshipType type) {
        Relationship existingRelationship = relationshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relationship with id " + id + " not found"));
        existingRelationship.setPerson(person);
        existingRelationship.setRelatedPerson(relatedPerson);
        existingRelationship.setRelationshipType(type);

        return relationshipRepository.save(existingRelationship);
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
        if (rootPerson == null){
            return  null;
        }
        Map <String, Object> node = new LinkedHashMap<>();

        String fullName = rootPerson.getFirstName();
        if (rootPerson.getMiddleName() !=null && !rootPerson.getMiddleName().isBlank()){
            fullName = fullName + " " + rootPerson.getMiddleName();
        }

        node.put("id", rootPerson.getId());
        node.put("dbId", rootPerson.getId());
        node.put("parentDbId", null);
        node.put("generationNumber", rootPerson.getGenerationNumber());
        node.put("name", fullName);

        List<Map<String, Object>> children = new java.util.ArrayList<>();
        for (Person child: getDirectChildren(rootPerson)){
            Map<String, Object> childNode = buildLineageTree(child);
            if (childNode != null){
                childNode.put("parentDbId", rootPerson.getId());
                children.add(childNode);
            }
        }
        node.put("children", children);
        return node;
    }
}
