package com.familytree.controller;

import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.services.PersonCorrectionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin review queue for member-submitted PersonCorrectionRequests --
 * see docs/08 Phase 4. Restricted to ROLE_ADMIN in SecurityConfig.
 * Thymeleaf, matching AdminVerificationController -- one list page
 * with inline approve/reject, no separate detail page, since a
 * correction request is a single field/value/reason, unlike a signup
 * request's much larger submitted profile.
 */
@Controller
public class AdminCorrectionController {

    private final PersonCorrectionRequestRepository correctionRequestRepository;
    private final PersonCorrectionService personCorrectionService;

    public AdminCorrectionController(PersonCorrectionRequestRepository correctionRequestRepository,
                                     PersonCorrectionService personCorrectionService) {
        this.correctionRequestRepository = correctionRequestRepository;
        this.personCorrectionService = personCorrectionService;
    }

    @GetMapping("/admin/corrections")
    public String list(@RequestParam(required = false) CorrectionRequestStatus status, Model model) {
        CorrectionRequestStatus selectedStatus = status != null ? status : CorrectionRequestStatus.PENDING;

        model.addAttribute("requests", correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(selectedStatus));
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("statuses", CorrectionRequestStatus.values());
        return "admin-corrections";
    }

    @PostMapping("/admin/corrections/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String decisionNote,
                          Authentication authentication) {
        personCorrectionService.approve(id, authentication.getName(), decisionNote);
        return "redirect:/admin/corrections";
    }

    @PostMapping("/admin/corrections/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String decisionNote,
                         Authentication authentication) {
        personCorrectionService.reject(id, authentication.getName(), decisionNote);
        return "redirect:/admin/corrections";
    }
}
