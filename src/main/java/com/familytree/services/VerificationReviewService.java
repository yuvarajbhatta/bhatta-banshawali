package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
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
    private final PersonRepository personRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final AuditLogService auditLogService;

    public VerificationReviewService(VerificationRequestRepository verificationRequestRepository,
                                     UserAccountRepository userAccountRepository,
                                     RoleRepository roleRepository,
                                     PersonRepository personRepository,
                                     UserPersonLinkRepository userPersonLinkRepository,
                                     AuditLogService auditLogService) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.personRepository = personRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * @param linkedPersonId the existing Person record the admin has confirmed this
     *                        applicant is, from the candidates shown in the review UI
     *                        (docs/04-data-model.md UserPersonLink) -- null when no
     *                        candidate matched (the applicant is a genuinely new person,
     *                        or the admin couldn't confirm one). Approval still proceeds
     *                        either way; only the link is skipped.
     */
    @Transactional
    public void approve(Long verificationRequestId, String reviewerUsername, String decisionNote, Long linkedPersonId) {
        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.APPROVED, reviewerUsername, decisionNote);

        UserAccount account = request.getUserAccount();
        account.setStatus(UserAccountStatus.ACTIVE);
        roleRepository.findByName(VERIFIED_MEMBER_ROLE).ifPresent(role -> account.getRoles().add(role));
        userAccountRepository.save(account);

        if (linkedPersonId != null) {
            Person person = personRepository.findById(linkedPersonId)
                    .orElseThrow(() -> new RuntimeException("Person not found with id: " + linkedPersonId));
            UserPersonLink link = new UserPersonLink();
            link.setUserAccount(account);
            link.setPerson(person);
            link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
            link.setVerifiedAt(LocalDateTime.now());
            userPersonLinkRepository.save(link);
        }

        auditLogService.record(AuditLogService.ACTION_SIGNUP_APPROVED, AuditLogService.ENTITY_VERIFICATION_REQUEST,
                verificationRequestId, "Approved signup for " + request.getSubmittedFullName(), reviewerUsername);
    }

    @Transactional
    public void reject(Long verificationRequestId, String reviewerUsername, String decisionNote) {
        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.REJECTED, reviewerUsername, decisionNote);

        UserAccount account = request.getUserAccount();
        account.setStatus(UserAccountStatus.DISABLED);
        userAccountRepository.save(account);

        auditLogService.record(AuditLogService.ACTION_SIGNUP_REJECTED, AuditLogService.ENTITY_VERIFICATION_REQUEST,
                verificationRequestId, "Rejected signup for " + request.getSubmittedFullName(), reviewerUsername);
    }

    @Transactional
    public void requestMoreInfo(Long verificationRequestId, String reviewerUsername, String decisionNote) {
        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.NEEDS_MORE_INFO, reviewerUsername, decisionNote);
        // UserAccount status is left as-is: the applicant hasn't been
        // rejected, just asked for more information before a decision.

        auditLogService.record(AuditLogService.ACTION_SIGNUP_MORE_INFO_REQUESTED, AuditLogService.ENTITY_VERIFICATION_REQUEST,
                verificationRequestId, "Requested more info for signup from " + request.getSubmittedFullName(), reviewerUsername);
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
