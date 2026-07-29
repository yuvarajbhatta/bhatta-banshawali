package com.familytree.controller;

import com.familytree.dto.AdminRelationshipDto;
import com.familytree.dto.AdminRelationshipRequestDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipCycleException;
import com.familytree.services.RelationshipService;
import com.familytree.web.PersonDisplayHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST replacement for RelationshipController's Thymeleaf CRUD pages
 * (docs/08 Phase 6), reusing RelationshipService's cycle-checked,
 * auto-linking save logic -- same rules /relationships/new already
 * enforces (same-person rejection, duplicate rejection, ancestor-cycle
 * rejection). Admin-only via "/api/v1/admin/**" in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/relationships")
public class AdminRelationshipApiController {

    private final RelationshipService relationshipService;
    private final PersonService personService;
    private final PersonDisplayHelper personDisplay;

    public AdminRelationshipApiController(RelationshipService relationshipService, PersonService personService,
                                          PersonDisplayHelper personDisplay) {
        this.relationshipService = relationshipService;
        this.personService = personService;
        this.personDisplay = personDisplay;
    }

    @GetMapping
    public List<AdminRelationshipDto> list() {
        return relationshipService.getAllRelationships().stream().map(this::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<AdminRelationshipDto> create(@Valid @RequestBody AdminRelationshipRequestDto request) {
        Person person = getPersonOrThrow(request.getPersonId());
        Person relatedPerson = getPersonOrThrow(request.getRelatedPersonId());

        if (person.getId().equals(relatedPerson.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A person cannot have a relationship with themselves.");
        }
        if (relationshipService.relationshipExists(person, relatedPerson, request.getRelationshipType())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This relationship already exists.");
        }

        try {
            relationshipService.saveRelationshipWithAutoLinks(person, relatedPerson, request.getRelationshipType());
        } catch (RelationshipCycleException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }

        // saveRelationshipWithAutoLinks returns void -- re-read the row it
        // just wrote (scoped to this person+type, not a full table scan).
        Relationship saved = relationshipService.getRelationshipsByPersonAndType(person, request.getRelationshipType()).stream()
                .filter(r -> r.getRelatedPerson().getId().equals(relatedPerson.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Relationship was saved but could not be re-read."));

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public AdminRelationshipDto update(@PathVariable Long id, @Valid @RequestBody AdminRelationshipRequestDto request) {
        getRelationshipOrThrow(id);
        Person person = getPersonOrThrow(request.getPersonId());
        Person relatedPerson = getPersonOrThrow(request.getRelatedPersonId());

        if (person.getId().equals(relatedPerson.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A person cannot have a relationship with themselves.");
        }

        try {
            Relationship updated = relationshipService.updateRelationship(id, person, relatedPerson, request.getRelationshipType());
            return toDto(updated);
        } catch (RelationshipCycleException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        getRelationshipOrThrow(id);
        relationshipService.deleteRelationshipById(id);
        return ResponseEntity.noContent().build();
    }

    private Person getPersonOrThrow(Long id) {
        try {
            return personService.getPersonById(id);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + id);
        }
    }

    private Relationship getRelationshipOrThrow(Long id) {
        try {
            return relationshipService.getRelationshipById(id);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship not found with id: " + id);
        }
    }

    private AdminRelationshipDto toDto(Relationship relationship) {
        return new AdminRelationshipDto(
                relationship.getId(),
                relationship.getPerson().getId(),
                personDisplay.englishFullName(relationship.getPerson()),
                relationship.getRelatedPerson().getId(),
                personDisplay.englishFullName(relationship.getRelatedPerson()),
                relationship.getRelationshipType()
        );
    }
}
