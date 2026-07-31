package com.familytree.controller;

import com.familytree.dto.DuplicateCandidateDto;
import com.familytree.dto.MergeRequestDto;
import com.familytree.dto.MergeResultDto;
import com.familytree.services.DuplicateCandidateService;
import com.familytree.services.PersonMergeConflictException;
import com.familytree.services.PersonMergeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Duplicate-person candidate detection and guided merge (docs/08 Phase 6).
 * Merge is never automatic -- an admin reviews each candidate pair and
 * explicitly picks which record survives. Admin-only via "/api/v1/admin/**".
 */
@RestController
@RequestMapping("/api/v1/admin/duplicates")
public class AdminDuplicatesApiController {

    private final DuplicateCandidateService duplicateCandidateService;
    private final PersonMergeService personMergeService;

    public AdminDuplicatesApiController(DuplicateCandidateService duplicateCandidateService,
                                        PersonMergeService personMergeService) {
        this.duplicateCandidateService = duplicateCandidateService;
        this.personMergeService = personMergeService;
    }

    @GetMapping
    public List<DuplicateCandidateDto> list() {
        return duplicateCandidateService.findCandidates();
    }

    @PostMapping("/merge")
    public MergeResultDto merge(@Valid @RequestBody MergeRequestDto request, Authentication authentication) {
        try {
            return personMergeService.merge(request.getSurvivorId(), request.getLoserId(), authentication.getName());
        } catch (PersonMergeConflictException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }
}
