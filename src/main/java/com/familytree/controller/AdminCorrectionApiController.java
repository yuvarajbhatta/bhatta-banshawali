package com.familytree.controller;

import com.familytree.dto.AdminCorrectionSummaryDto;
import com.familytree.dto.AdminDecisionRequestDto;
import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.services.PersonCorrectionService;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST replacement for AdminCorrectionController's Thymeleaf page
 * (docs/08 Phase 6), reusing PersonCorrectionService so approve/reject
 * behave identically to the existing admin-corrections.html flow.
 * Admin-only via the existing "/api/v1/admin/**" matcher.
 */
@RestController
@RequestMapping("/api/v1/admin/corrections")
public class AdminCorrectionApiController {

    private final PersonCorrectionRequestRepository correctionRequestRepository;
    private final PersonCorrectionService personCorrectionService;
    private final PersonDisplayHelper personDisplay;

    public AdminCorrectionApiController(PersonCorrectionRequestRepository correctionRequestRepository,
                                        PersonCorrectionService personCorrectionService,
                                        PersonDisplayHelper personDisplay) {
        this.correctionRequestRepository = correctionRequestRepository;
        this.personCorrectionService = personCorrectionService;
        this.personDisplay = personDisplay;
    }

    @GetMapping
    public List<AdminCorrectionSummaryDto> list(@RequestParam(required = false) CorrectionRequestStatus status) {
        CorrectionRequestStatus selectedStatus = status != null ? status : CorrectionRequestStatus.PENDING;
        return correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(selectedStatus).stream()
                .map(this::toSummary)
                .toList();
    }

    @PostMapping("/{id}/approve")
    public AdminCorrectionSummaryDto approve(@PathVariable Long id, @RequestBody(required = false) AdminDecisionRequestDto body,
                                             Authentication authentication) {
        AdminDecisionRequestDto decision = body != null ? body : new AdminDecisionRequestDto();
        personCorrectionService.approve(id, authentication.getName(), decision.getDecisionNote());
        return toSummary(getOrThrow(id));
    }

    @PostMapping("/{id}/reject")
    public AdminCorrectionSummaryDto reject(@PathVariable Long id, @RequestBody(required = false) AdminDecisionRequestDto body,
                                            Authentication authentication) {
        AdminDecisionRequestDto decision = body != null ? body : new AdminDecisionRequestDto();
        personCorrectionService.reject(id, authentication.getName(), decision.getDecisionNote());
        return toSummary(getOrThrow(id));
    }

    private AdminCorrectionSummaryDto toSummary(PersonCorrectionRequest request) {
        return new AdminCorrectionSummaryDto(
                request.getId(),
                request.getPerson().getId(),
                personDisplay.englishFullName(request.getPerson()),
                request.getField(),
                request.getCurrentValueSnapshot(),
                request.getProposedValue(),
                request.getReason(),
                request.getSubmittedBy().getEmail(),
                request.getSubmittedAt(),
                request.getStatus(),
                request.getReviewedByUsername(),
                request.getReviewedAt(),
                request.getDecisionNote()
        );
    }

    private PersonCorrectionRequest getOrThrow(Long id) {
        return correctionRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Correction request not found with id: " + id));
    }
}
