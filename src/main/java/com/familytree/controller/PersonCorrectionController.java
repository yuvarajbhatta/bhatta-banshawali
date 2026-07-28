package com.familytree.controller;

import com.familytree.dto.CorrectionRequestDto;
import com.familytree.dto.CorrectionResponseDto;
import com.familytree.services.PersonCorrectionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member-submitted corrections for a Person's fields (docs/08 Phase 4).
 * Authenticated-only (anyRequest().authenticated() in SecurityConfig) --
 * requires a UserAccount, so an AppUser admin session gets the same
 * "No member profile for this account" 404 as GET /api/v1/me.
 */
@RestController
@RequestMapping("/api/v1/persons/{id}/corrections")
public class PersonCorrectionController {

    private final PersonCorrectionService personCorrectionService;

    public PersonCorrectionController(PersonCorrectionService personCorrectionService) {
        this.personCorrectionService = personCorrectionService;
    }

    @PostMapping
    public CorrectionResponseDto submit(@PathVariable Long id, @Valid @RequestBody CorrectionRequestDto request,
                                        Authentication authentication) {
        personCorrectionService.submit(id, request.getField(), request.getProposedValue(), request.getReason(),
                authentication.getName());
        return CorrectionResponseDto.submitted();
    }
}
