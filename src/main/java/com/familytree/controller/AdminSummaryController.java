package com.familytree.controller;

import com.familytree.dto.AdminSummaryDto;
import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.AdminAccessRequestService;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only admin dashboard summary (docs/08 Phase 4 UI overhaul) --
 * counts and a short recent list from each pending review queue.
 * ROLE_ADMIN only (see SecurityConfig's /api/v1/admin/** rule). Actual
 * review/decision actions still happen on the existing /admin/signups
 * and /admin/corrections Thymeleaf pages; this just summarizes them for
 * the new dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin/summary")
public class AdminSummaryController {

    private static final int RECENT_LIMIT = 5;

    private final VerificationRequestRepository verificationRequestRepository;
    private final PersonCorrectionRequestRepository correctionRequestRepository;
    private final AdminAccessRequestService adminAccessRequestService;
    private final PersonDisplayHelper personDisplay;

    public AdminSummaryController(VerificationRequestRepository verificationRequestRepository,
                                  PersonCorrectionRequestRepository correctionRequestRepository,
                                  AdminAccessRequestService adminAccessRequestService,
                                  PersonDisplayHelper personDisplay) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.correctionRequestRepository = correctionRequestRepository;
        this.adminAccessRequestService = adminAccessRequestService;
        this.personDisplay = personDisplay;
    }

    @GetMapping
    public AdminSummaryDto summary() {
        List<VerificationRequest> pendingSignups =
                verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING);
        List<PersonCorrectionRequest> pendingCorrections =
                correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.PENDING);

        List<AdminSummaryDto.PendingSignupSummary> recentSignups = pendingSignups.stream()
                .limit(RECENT_LIMIT)
                .map(request -> new AdminSummaryDto.PendingSignupSummary(
                        request.getId(), request.getSubmittedFullName(), request.getCreatedAt()))
                .toList();

        List<AdminSummaryDto.PendingCorrectionSummary> recentCorrections = pendingCorrections.stream()
                .limit(RECENT_LIMIT)
                .map(request -> new AdminSummaryDto.PendingCorrectionSummary(
                        request.getId(),
                        personDisplay.englishFullName(request.getPerson()),
                        request.getField().name(),
                        request.getSubmittedAt()))
                .toList();

        return new AdminSummaryDto(pendingSignups.size(), pendingCorrections.size(),
                adminAccessRequestService.pendingCount(), recentSignups, recentCorrections);
    }
}
