package com.familytree.controller;

import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.PersonService;
import com.familytree.services.ViewerContext;
import com.familytree.services.ViewerContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Member-facing directory: search and person detail (docs/08 Phase 4).
 * Both authenticated-only (anyRequest().authenticated() in
 * SecurityConfig) -- same access level the existing Thymeleaf
 * /persons and /persons/{id} pages already grant any logged-in
 * ADMIN or USER, not a new privacy boundary. Sensitive fields
 * (birth date, current address) are separately redacted per-viewer by
 * PersonProfileAssembler.
 */
@RestController
@RequestMapping("/api/v1/persons")
public class PersonApiController {

    private final PersonService personService;
    private final PersonProfileAssembler personProfileAssembler;
    private final ViewerContextResolver viewerContextResolver;

    public PersonApiController(PersonService personService, PersonProfileAssembler personProfileAssembler,
                               ViewerContextResolver viewerContextResolver) {
        this.personService = personService;
        this.personProfileAssembler = personProfileAssembler;
        this.viewerContextResolver = viewerContextResolver;
    }

    @GetMapping
    public List<PersonSummaryDto> search(@RequestParam(required = false) String keyword, Authentication authentication) {
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        return personService.searchPersons(keyword).stream()
                .map(person -> personProfileAssembler.summarize(person, viewer))
                .toList();
    }

    @GetMapping("/{id}")
    public PersonDetailDto detail(@PathVariable Long id, Authentication authentication) {
        Person person;
        try {
            person = personService.getPersonById(id);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + id);
        }
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        return personProfileAssembler.detail(person, viewer);
    }
}
