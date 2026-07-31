package com.familytree.controller;

import com.familytree.entity.Person;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.CommaSeparatedIds;
import com.familytree.services.VerificationReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Admin review queue for signup VerificationRequests -- see
 * docs/05-auth-and-verification.md. Restricted to ROLE_ADMIN in
 * SecurityConfig. Thymeleaf, matching the existing admin tooling
 * (PersonController, RelationshipController), not Next.js -- this is
 * privileged internal tooling, not part of the public-facing rewrite.
 */
@Controller
public class AdminVerificationController {

    private final VerificationRequestRepository verificationRequestRepository;
    private final PersonRepository personRepository;
    private final VerificationReviewService verificationReviewService;

    public AdminVerificationController(VerificationRequestRepository verificationRequestRepository,
                                       PersonRepository personRepository,
                                       VerificationReviewService verificationReviewService) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.personRepository = personRepository;
        this.verificationReviewService = verificationReviewService;
    }

    @GetMapping("/admin/signups")
    public String list(@RequestParam(required = false) VerificationStatus status, Model model) {
        VerificationStatus selectedStatus = status != null ? status : VerificationStatus.PENDING;

        model.addAttribute("requests", verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(selectedStatus));
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("statuses", VerificationStatus.values());
        return "admin-signups";
    }

    @GetMapping("/admin/signups/{id}")
    public String detail(@PathVariable Long id, Model model) {
        VerificationRequest request = verificationRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found with id: " + id));

        model.addAttribute("request", request);
        model.addAttribute("candidatePersons", resolveCandidatePersons(request.getMatchedCandidatePersonIds()));
        model.addAttribute("fatherCandidatePersons", resolveCandidatePersons(request.getMatchedFatherCandidatePersonIds()));
        return "admin-signup-detail";
    }

    @PostMapping("/admin/signups/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String decisionNote,
                          @RequestParam(required = false) Long linkedPersonId,
                          @RequestParam(required = false) Long createAsChildOfFatherId,
                          Authentication authentication) {
        verificationReviewService.approve(id, authentication.getName(), decisionNote, linkedPersonId, createAsChildOfFatherId);
        return "redirect:/admin/signups";
    }

    @PostMapping("/admin/signups/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String decisionNote,
                        Authentication authentication) {
        verificationReviewService.reject(id, authentication.getName(), decisionNote);
        return "redirect:/admin/signups";
    }

    @PostMapping("/admin/signups/{id}/request-more-info")
    public String requestMoreInfo(@PathVariable Long id, @RequestParam(required = false) String decisionNote,
                                  Authentication authentication) {
        verificationReviewService.requestMoreInfo(id, authentication.getName(), decisionNote);
        return "redirect:/admin/signups";
    }

    private List<Person> resolveCandidatePersons(String matchedCandidatePersonIds) {
        return personRepository.findAllById(CommaSeparatedIds.parse(matchedCandidatePersonIds));
    }
}
