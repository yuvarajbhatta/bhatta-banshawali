package com.familytree.controller;

import com.familytree.entity.RelationshipType;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RelationshipController {

    private final RelationshipService relationshipService;
    private final PersonService personService;

    public RelationshipController(RelationshipService relationshipService, PersonService personService) {
        this.relationshipService = relationshipService;
        this.personService = personService;
    }

    @GetMapping("/relationships/new")
    public String showAddRelationshipForm(Model model) {
        if (!model.containsAttribute("personId")) {
            model.addAttribute("personId", null);
        }
        if (!model.containsAttribute("relatedPersonId")) {
            model.addAttribute("relatedPersonId", null);
        }
        if (!model.containsAttribute("relationshipTypeValue")) {
            model.addAttribute("relationshipTypeValue", "");
        }
        if (!model.containsAttribute("editMode")) {
            model.addAttribute("editMode", false);
        }
        if (!model.containsAttribute("relationshipId")) {
            model.addAttribute("relationshipId", null);
        }

        model.addAttribute("persons", personService.getAllPersons());
        model.addAttribute("relationshipTypes", RelationshipType.values());
        return "add-relationship";
    }

    @GetMapping("/relationships/edit/{id}")
    public String showEditRelationshipForm(@PathVariable Long id, Model model) {
        var relationship = relationshipService.getRelationshipById(id);

        model.addAttribute("relationshipId", relationship.getId());
        model.addAttribute("personId", relationship.getPerson().getId());
        model.addAttribute("relatedPersonId", relationship.getRelatedPerson().getId());
        model.addAttribute("relationshipTypeValue", relationship.getRelationshipType().name());
        model.addAttribute("editMode", true);
        model.addAttribute("persons", personService.getAllPersons());
        model.addAttribute("relationshipTypes", RelationshipType.values());

        return "add-relationship";
    }

    @PostMapping("/relationships")
    public String saveRelationship(@RequestParam(required = false) Long personId,
                                   @RequestParam(required = false) Long relatedPersonId,
                                   @RequestParam(required = false) String relationshipType,
                                   Model model) {

        boolean hasError = false;

        model.addAttribute("personId", personId);
        model.addAttribute("relatedPersonId", relatedPersonId);
        model.addAttribute("relationshipTypeValue", relationshipType);
        model.addAttribute("persons", personService.getAllPersons());
        model.addAttribute("relationshipTypes", RelationshipType.values());
        model.addAttribute("editMode", false);
        model.addAttribute("relationshipId", null);

        if (personId == null) {
            model.addAttribute("personIdError", "Please select a person.");
            hasError = true;
        }

        if (relatedPersonId == null) {
            model.addAttribute("relatedPersonIdError", "Please select a related person.");
            hasError = true;
        }

        if (relationshipType == null || relationshipType.isBlank()) {
            model.addAttribute("relationshipTypeError", "Please select a relationship type.");
            hasError = true;
        }

        if (personId != null && relatedPersonId != null && personId.equals(relatedPersonId)) {
            model.addAttribute("samePersonError", "Person and related person cannot be the same.");
            hasError = true;
        }

        if (hasError) {
            return "add-relationship";
        }

        var person = personService.getPersonById(personId);
        var relatedPerson = personService.getPersonById(relatedPersonId);
        var type = RelationshipType.valueOf(relationshipType);

        boolean duplicateExists = relationshipService.relationshipExists(person, relatedPerson, type);
        if (duplicateExists) {
            model.addAttribute("duplicateRelationshipError", "This relationship already exists.");
            return "add-relationship";
        }

        relationshipService.saveRelationshipWithAutoLinks(person, relatedPerson, type);
        return "redirect:/relationships";
    }

    @PostMapping("/relationships/update/{id}")
    public String updateRelationship(@PathVariable Long id,
                                     @RequestParam(required = false) Long personId,
                                     @RequestParam(required = false) Long relatedPersonId,
                                     @RequestParam(required = false) String relationshipType,
                                     Model model) {

        boolean hasError = false;

        model.addAttribute("relationshipId", id);
        model.addAttribute("personId", personId);
        model.addAttribute("relatedPersonId", relatedPersonId);
        model.addAttribute("relationshipTypeValue", relationshipType);
        model.addAttribute("persons", personService.getAllPersons());
        model.addAttribute("relationshipTypes", RelationshipType.values());
        model.addAttribute("editMode", true);

        if (personId == null) {
            model.addAttribute("personIdError", "Please select a person.");
            hasError = true;
        }

        if (relatedPersonId == null) {
            model.addAttribute("relatedPersonIdError", "Please select a related person.");
            hasError = true;
        }

        if (relationshipType == null || relationshipType.isBlank()) {
            model.addAttribute("relationshipTypeError", "Please select a relationship type.");
            hasError = true;
        }

        if (personId != null && relatedPersonId != null && personId.equals(relatedPersonId)) {
            model.addAttribute("samePersonError", "Person and related person cannot be the same.");
            hasError = true;
        }

        if (hasError) {
            return "add-relationship";
        }

        var person = personService.getPersonById(personId);
        var relatedPerson = personService.getPersonById(relatedPersonId);
        var type = RelationshipType.valueOf(relationshipType);

        relationshipService.updateRelationship(id, person, relatedPerson, type);
        return "redirect:/relationships";
    }

    @GetMapping("/relationships")
    public String listRelationships(Model model) {
        model.addAttribute("relationships", relationshipService.getAllRelationships());
        return "relationships";
    }

    @GetMapping("/relationships/delete/{id}")
    public String deleteRelationship(@PathVariable Long id) {
        relationshipService.deleteRelationshipById(id);
        return "redirect:/relationships";
    }
}