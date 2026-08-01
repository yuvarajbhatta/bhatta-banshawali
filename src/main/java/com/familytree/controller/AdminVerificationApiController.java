package com.familytree.controller;

import com.familytree.dto.AdminSignupDecisionRequestDto;
import com.familytree.dto.AdminSignupDetailDto;
import com.familytree.dto.AdminSignupSummaryDto;
import com.familytree.dto.MatchCandidateDto;
import com.familytree.entity.Person;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.CommaSeparatedIds;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.VerificationReviewService;
import com.familytree.services.ViewerContext;
import com.familytree.services.ViewerContextResolver;
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
 * REST replacement for AdminVerificationController's Thymeleaf pages
 * (docs/08 Phase 6), reusing the exact same VerificationReviewService
 * so approve/reject/request-more-info behave identically either way.
 * Admin-only via the existing "/api/v1/admin/**" -> hasRole("ADMIN")
 * matcher in SecurityConfig -- no new security rule needed. The
 * Thymeleaf pages stay in place for now (docs/frontend-redesign-plan.md
 * migration approach: additive, not a hard cutover).
 */
@RestController
@RequestMapping("/api/v1/admin/signups")
public class AdminVerificationApiController {

    private final VerificationRequestRepository verificationRequestRepository;
    private final PersonRepository personRepository;
    private final VerificationReviewService verificationReviewService;
    private final PersonProfileAssembler personProfileAssembler;
    private final ViewerContextResolver viewerContextResolver;

    public AdminVerificationApiController(VerificationRequestRepository verificationRequestRepository,
                                          PersonRepository personRepository,
                                          VerificationReviewService verificationReviewService,
                                          PersonProfileAssembler personProfileAssembler,
                                          ViewerContextResolver viewerContextResolver) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.personRepository = personRepository;
        this.verificationReviewService = verificationReviewService;
        this.personProfileAssembler = personProfileAssembler;
        this.viewerContextResolver = viewerContextResolver;
    }

    @GetMapping
    public List<AdminSignupSummaryDto> list(@RequestParam(required = false) VerificationStatus status) {
        VerificationStatus selectedStatus = status != null ? status : VerificationStatus.PENDING;
        return verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(selectedStatus).stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/{id}")
    public AdminSignupDetailDto detail(@PathVariable Long id, Authentication authentication) {
        VerificationRequest request = getOrThrow(id);
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        List<MatchCandidateDto> candidates = toMatchCandidates(
                resolveCandidatePersons(request.getMatchedCandidatePersonIds()), viewer);
        List<MatchCandidateDto> fatherCandidates = toMatchCandidates(
                resolveCandidatePersons(request.getMatchedFatherCandidatePersonIds()), viewer);
        return toDetail(request, candidates, fatherCandidates);
    }

    // Each candidate's ancestorChain (the FATHER line, as far back as it's
    // recorded) is what actually lets the admin tell same-named candidates
    // apart -- see PersonProfileAssembler.ancestorChain.
    private List<MatchCandidateDto> toMatchCandidates(List<Person> persons, ViewerContext viewer) {
        return persons.stream()
                .map(person -> new MatchCandidateDto(
                        personProfileAssembler.summarize(person, viewer),
                        personProfileAssembler.ancestorChain(person, viewer)))
                .toList();
    }

    @PostMapping("/{id}/approve")
    public AdminSignupDetailDto approve(@PathVariable Long id, @RequestBody(required = false) AdminSignupDecisionRequestDto body,
                                        Authentication authentication) {
        AdminSignupDecisionRequestDto decision = body != null ? body : new AdminSignupDecisionRequestDto();
        verificationReviewService.approve(id, authentication.getName(), decision.getDecisionNote(),
                decision.getLinkedPersonId(), decision.getCreateAsChildOfFatherId());
        return detail(id, authentication);
    }

    @PostMapping("/{id}/reject")
    public AdminSignupDetailDto reject(@PathVariable Long id, @RequestBody(required = false) AdminSignupDecisionRequestDto body,
                                       Authentication authentication) {
        AdminSignupDecisionRequestDto decision = body != null ? body : new AdminSignupDecisionRequestDto();
        verificationReviewService.reject(id, authentication.getName(), decision.getDecisionNote());
        return detail(id, authentication);
    }

    @PostMapping("/{id}/request-more-info")
    public AdminSignupDetailDto requestMoreInfo(@PathVariable Long id, @RequestBody(required = false) AdminSignupDecisionRequestDto body,
                                                Authentication authentication) {
        AdminSignupDecisionRequestDto decision = body != null ? body : new AdminSignupDecisionRequestDto();
        verificationReviewService.requestMoreInfo(id, authentication.getName(), decision.getDecisionNote());
        return detail(id, authentication);
    }

    private AdminSignupSummaryDto toSummary(VerificationRequest request) {
        return new AdminSignupSummaryDto(
                request.getId(),
                request.getSubmittedFullName(),
                request.getSubmittedFatherName(),
                request.getSubmittedGrandfatherName(),
                request.getMatchConfidence(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }

    private AdminSignupDetailDto toDetail(VerificationRequest request, List<MatchCandidateDto> candidates,
                                          List<MatchCandidateDto> fatherCandidates) {
        return new AdminSignupDetailDto(
                request.getId(),
                request.getSubmittedFullName(),
                request.getSubmittedFullNameNepali(),
                request.getSubmittedFatherName(),
                request.getSubmittedGrandfatherName(),
                request.getSubmittedDobAd(),
                request.getSubmittedDobBsYear(),
                request.getSubmittedDobBsMonth(),
                request.getSubmittedDobBsDay(),
                request.getMotherName(),
                request.getPlaceOfBirth(),
                request.getAncestralVillage(),
                request.getFamilyBranch(),
                request.getKnownRelativeName(),
                request.getInvitationCode(),
                request.getApplicantNote(),
                request.getMatchConfidence(),
                request.getStatus(),
                request.getReviewedByUsername(),
                request.getReviewedAt(),
                request.getDecisionNote(),
                request.getCreatedAt(),
                candidates,
                fatherCandidates
        );
    }

    private List<Person> resolveCandidatePersons(String matchedCandidatePersonIds) {
        return personRepository.findAllById(CommaSeparatedIds.parse(matchedCandidatePersonIds));
    }

    private VerificationRequest getOrThrow(Long id) {
        return verificationRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification request not found with id: " + id));
    }
}
