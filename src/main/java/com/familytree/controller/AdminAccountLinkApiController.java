package com.familytree.controller;

import com.familytree.dto.LinkAccountRequestDto;
import com.familytree.dto.UnlinkedAccountDto;
import com.familytree.entity.UserAccount;
import com.familytree.entity.VerificationRequest;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.VerificationReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Repairs the gap where a signup was approved without a candidate
 * being selected (or none matched), leaving an ACTIVE account with no
 * Person link and no way to fix it afterwards -- the review UI only
 * offers candidate-selection while a request is still PENDING. See
 * docs/frontend-redesign-plan.md. Admin-only via "/api/v1/admin/**".
 */
@RestController
@RequestMapping("/api/v1/admin/unlinked-accounts")
public class AdminAccountLinkApiController {

    private final VerificationReviewService verificationReviewService;
    private final VerificationRequestRepository verificationRequestRepository;

    public AdminAccountLinkApiController(VerificationReviewService verificationReviewService,
                                         VerificationRequestRepository verificationRequestRepository) {
        this.verificationReviewService = verificationReviewService;
        this.verificationRequestRepository = verificationRequestRepository;
    }

    @GetMapping
    public List<UnlinkedAccountDto> list() {
        return verificationReviewService.findUnlinkedActiveAccounts().stream().map(this::toDto).toList();
    }

    @PostMapping("/{userAccountId}/link")
    public void link(@PathVariable Long userAccountId, @Valid @RequestBody LinkAccountRequestDto request,
                     Authentication authentication) {
        verificationReviewService.linkAccountToPerson(userAccountId, request.getPersonId(), authentication.getName());
    }

    private UnlinkedAccountDto toDto(UserAccount account) {
        VerificationRequest mostRecent = verificationRequestRepository.findByUserAccountId(account.getId()).stream()
                .max(Comparator.comparing(VerificationRequest::getCreatedAt))
                .orElse(null);

        return new UnlinkedAccountDto(
                account.getId(),
                account.getEmail(),
                account.getCreatedAt(),
                mostRecent != null ? mostRecent.getSubmittedFullName() : null,
                mostRecent != null ? mostRecent.getSubmittedFatherName() : null,
                mostRecent != null ? mostRecent.getSubmittedGrandfatherName() : null
        );
    }
}
