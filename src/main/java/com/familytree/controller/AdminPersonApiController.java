package com.familytree.controller;

import com.familytree.dto.AdminPersonCreateResultDto;
import com.familytree.dto.AdminPersonDto;
import com.familytree.dto.AdminPersonRequestDto;
import com.familytree.entity.Person;
import com.familytree.services.DuplicateCandidateService;
import com.familytree.services.PersonService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST replacement for PersonController's Thymeleaf CRUD pages
 * (docs/08 Phase 6), reusing PersonService directly -- same
 * save/update/delete logic as /persons, /persons/new, etc. Admin-only
 * via "/api/v1/admin/**" -> hasRole("ADMIN") in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/persons")
public class AdminPersonApiController {

    private final PersonService personService;
    private final DuplicateCandidateService duplicateCandidateService;

    public AdminPersonApiController(PersonService personService, DuplicateCandidateService duplicateCandidateService) {
        this.personService = personService;
        this.duplicateCandidateService = duplicateCandidateService;
    }

    @GetMapping
    public List<AdminPersonDto> list(@RequestParam(required = false) String keyword) {
        return personService.searchPersons(keyword).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public AdminPersonDto detail(@PathVariable Long id) {
        return toDto(getOrThrow(id));
    }

    @PostMapping
    public ResponseEntity<AdminPersonCreateResultDto> create(@Valid @RequestBody AdminPersonRequestDto request) {
        Person person = toEntity(request);
        // Checked before saving -- findLikelyDuplicatesOf excludes the
        // candidate's own id, which doesn't exist yet either way, but
        // checking pre-save keeps this a pure read with no risk of the
        // freshly-saved row matching itself.
        List<AdminPersonDto> possibleDuplicates = duplicateCandidateService.findLikelyDuplicatesOf(person).stream()
                .map(this::toDto)
                .toList();
        Person saved = personService.savePerson(person);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AdminPersonCreateResultDto(toDto(saved), possibleDuplicates));
    }

    @PutMapping("/{id}")
    public AdminPersonDto update(@PathVariable Long id, @Valid @RequestBody AdminPersonRequestDto request) {
        getOrThrow(id);
        Person updated = personService.updatePerson(id, toEntity(request));
        return toDto(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        getOrThrow(id);
        personService.deletePersonById(id);
        return ResponseEntity.noContent().build();
    }

    private Person getOrThrow(Long id) {
        try {
            return personService.getPersonById(id);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + id);
        }
    }

    private Person toEntity(AdminPersonRequestDto request) {
        Person person = new Person();
        person.setGenerationNumber(request.getGenerationNumber());
        person.setFirstName(request.getFirstName());
        person.setFirstNameNepali(request.getFirstNameNepali());
        person.setMiddleName(request.getMiddleName());
        person.setMiddleNameNepali(request.getMiddleNameNepali());
        person.setLastName(request.getLastName());
        person.setLastNameNepali(request.getLastNameNepali());
        person.setNickname(request.getNickname());
        person.setGender(request.getGender());
        person.setBirthDate(request.getBirthDate());
        person.setDeathDate(request.getDeathDate());
        person.setPhotoPath(request.getPhotoPath());
        person.setBirthPlace(request.getBirthPlace());
        person.setCurrentAddress(request.getCurrentAddress());
        person.setGotra(request.getGotra());
        person.setNotes(request.getNotes());
        return person;
    }

    private AdminPersonDto toDto(Person person) {
        return new AdminPersonDto(
                person.getId(),
                person.getGenerationNumber(),
                person.getFirstName(),
                person.getFirstNameNepali(),
                person.getMiddleName(),
                person.getMiddleNameNepali(),
                person.getLastName(),
                person.getLastNameNepali(),
                person.getNickname(),
                person.getGender(),
                person.getBirthDate(),
                person.getDeathDate(),
                person.getPhotoPath(),
                person.getBirthPlace(),
                person.getCurrentAddress(),
                person.getGotra(),
                person.getNotes()
        );
    }
}
