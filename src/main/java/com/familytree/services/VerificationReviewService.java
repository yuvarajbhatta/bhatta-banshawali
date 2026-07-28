package com.familytree.services;

import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Admin actions on a signup VerificationRequest -- see
 * docs/05-auth-and-verification.md and docs/21 change management.
 */
@Service
public class VerificationReviewService {

    private static final String VERIFIED_MEMBER_ROLE = "VERIFIED_MEMBER";

    private final VerificationRequestRepository verificationRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    public VerificationReviewService(VerificationRequestRepository verificationRequestRepository,
                                     UserAccountRepository userAccountRepository,
                                     RoleRepository roleRepository) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void approve(Long verificationRequestId, String reviewerUsername, String decisionNote) {
        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.APPROVED, reviewerUsername, decisionNote);

        UserAccount account = request.getUserAccount();
        account.setStatus(UserAccountStatus.ACTIVE);
        roleRepository.findByName(VERIFIED_MEMBER_ROLE).ifPresent(role -> account.getRoles().add(role));
        userAccountRepository.save(account);
    }

    @Transactional
    public void reject(Long verificationRequestId, String reviewerUsername, String decisionNote) {
        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.REJECTED, reviewerUsername, decisionNote);

        UserAccount account = request.getUserAccount();
        account.setStatus(UserAccountStatus.DISABLED);
        userAccountRepository.save(account);
    }

    @Transactional
    public void requestMoreInfo(Long verificationRequestId, String reviewerUsername, String decisionNote) {
        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.NEEDS_MORE_INFO, reviewerUsername, decisionNote);
        // UserAccount status is left as-is: the applicant hasn't been
        // rejected, just asked for more information before a decision.
    }

    private void markReviewed(VerificationRequest request, VerificationStatus status, String reviewerUsername,
                              String decisionNote) {
        request.setStatus(status);
        request.setReviewedByUsername(reviewerUsername);
        request.setReviewedAt(LocalDateTime.now());
        request.setDecisionNote(decisionNote);
        verificationRequestRepository.save(request);
    }

    private VerificationRequest getOrThrow(Long id) {
        return verificationRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found with id: " + id));
    }
}
