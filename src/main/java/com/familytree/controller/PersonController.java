package com.familytree.controller;

import com.familytree.entity.Person;
import com.familytree.entity.RelationshipType;
import com.familytree.services.PersonService;
import com.familytree.services.RelationshipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
public class PersonController {
    private final PersonService personService;
    private final RelationshipService relationshipService;

    public PersonController(PersonService personService, RelationshipService relationshipService) {
        this.personService = personService;
        this.relationshipService = relationshipService;
    }

    @GetMapping("/persons/new")
    public String showAddPersonForm(Model model) {
        model.addAttribute("person", new Person());
        return "add-person";
    }

    @PostMapping("/persons")
    public String savePerson(@Valid Person person, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "add-person";
        }
        personService.savePerson(person);
        return "redirect:/persons";
    }

    @GetMapping("/persons/edit/{id}")
    public String showEditPersonForm(@PathVariable Long id, Model model) {
        model.addAttribute("person", personService.getPersonById(id));
        model.addAttribute("editMode", true);
        return "add-person";
    }

    @PostMapping("/persons/update/{id}")
    public String updatePerson(@PathVariable Long id,
                               @Valid Person person,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", true);
            return "add-person";
        }

        personService.updatePerson(id, person);
        return "redirect:/persons";
    }

    @GetMapping("/persons/delete/{id}")
    public String deletePerson(@PathVariable Long id) {
        personService.deletePersonById(id);
        return "redirect:/persons";
    }

    @GetMapping("/persons")
    public String listPersons(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("persons", personService.searchPersons(keyword));
        model.addAttribute("keyword", keyword);
        return "persons";
    }

    @GetMapping("/persons/{id}")
    public String viewPerson(@PathVariable Long id, Model model) {
        Person person = personService.getPersonById(id);

        model.addAttribute("person", person);
        model.addAttribute("fathers",
                relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER));
        model.addAttribute("mothers",
                relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER));
        model.addAttribute("spouses",
                relationshipService.getSpousesForPerson(person));
        model.addAttribute("children",
                relationshipService.getChildrenForPerson(person));
        return "person-details";
    }

    @GetMapping("/persons/transliterate")
    @ResponseBody
    public ResponseEntity<Map<String, String>> suggestNepaliNames(@RequestParam(required = false) String firstName,
                                                                  @RequestParam(required = false) String middleName,
                                                                  @RequestParam(required = false) String lastName,
                                                                  @RequestParam(required = false) String firstNameNepali,
                                                                  @RequestParam(required = false) String middleNameNepali,
                                                                  @RequestParam(required = false) String lastNameNepali) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setMiddleName(middleName);
        person.setLastName(lastName);
        person.setFirstNameNepali(firstNameNepali);
        person.setMiddleNameNepali(middleNameNepali);
        person.setLastNameNepali(lastNameNepali);

        Person suggestion = personService.suggestNepaliNames(person);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("firstNameNepali", suggestion.getFirstNameNepali());
        response.put("middleNameNepali", suggestion.getMiddleNameNepali());
        response.put("lastNameNepali", suggestion.getLastNameNepali());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generations")
    public String viewGenerations(Model model) {
        model.addAttribute("persons", personService.getAllPersonsOrderedByGeneration());
        return "generations";
    }

    @GetMapping("/lineage")
    public String viewLineage(Model model) {
//        Person rootPerson = relationshipService.getRootPersonForLineage();
//        model.addAttribute("rootPerson", rootPerson);
//
//        if (rootPerson != null) {
//            var children = relationshipService.getDirectChildren(rootPerson);
//            model.addAttribute("children", children);
//
//            java.util.Map<Long, java.util.List<Person>> grandchildrenByChild = new java.util.LinkedHashMap<>();
//            for (Person child: children) {
//                grandchildrenByChild.put(child.getId(), relationshipService.getDirectChildren(child));
//            }
//            model.addAttribute("grandchildrenByChild", grandchildrenByChild);
//        }

        return "lineage";
    }

    @GetMapping("/lineage/tree")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLineageTree(Locale locale) {
        Person rootPerson = relationshipService.getRootPersonForLineage();
        Map<String, Object> tree = relationshipService.buildLineageTree(rootPerson, locale);

        if (tree == null) {
            return ResponseEntity.ok(new LinkedHashMap<>());
        }
        return ResponseEntity.ok(tree);
    }


    @PostMapping("/lineage/save-person")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveLineagePerson(@RequestParam String fullName,
                                                                 @RequestParam(required = false) Long personId,
                                                                 @RequestParam(required = false) Long parentId,
                                                                 @RequestParam(required = false) Integer generationNumber) {
        Person savedPerson;

        if (personId != null) {
            savedPerson = personService.updateLineagePerson(personId, fullName, generationNumber);
        } else {
            savedPerson = personService.saveLineagePerson(fullName, generationNumber);

            if (parentId != null) {
                Person parent = personService.getPersonById(parentId);
                relationshipService.saveRelationshipWithAutoLinks(parent, savedPerson, RelationshipType.CHILD);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", savedPerson.getId());
        response.put("firstName", savedPerson.getFirstName());
        response.put("middleName", savedPerson.getMiddleName());
        response.put("lastName", savedPerson.getLastName());
        response.put("gender", savedPerson.getGender());
        response.put("generationNumber", savedPerson.getGenerationNumber());

        return ResponseEntity.ok(response);
    }

}
