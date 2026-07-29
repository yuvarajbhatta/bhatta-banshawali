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
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final PersonDisplayHelper personDisplay;

    public VerificationReviewService(VerificationRequestRepository verificationRequestRepository,
                                     UserAccountRepository userAccountRepository,
                                     RoleRepository roleRepository,
                                     PersonRepository personRepository,
                                     UserPersonLinkRepository userPersonLinkRepository,
                                     AuditLogService auditLogService,
                                     PersonDisplayHelper personDisplay) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.personRepository = personRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.auditLogService = auditLogService;
        this.personDisplay = personDisplay;
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

    /**
     * ACTIVE accounts (i.e. an approved signup) with no VERIFIED
     * UserPersonLink -- the gap that opens when an admin approves a
     * signup without selecting a candidate at the time (or the matcher
     * found nothing to select). See docs/frontend-redesign-plan.md
     * "link account" tool: previously there was no way to fix this
     * after the fact, since the candidate-selection UI only appeared
     * on a still-PENDING request.
     */
    public List<UserAccount> findUnlinkedActiveAccounts() {
        return userAccountRepository.findAll().stream()
                .filter(account -> account.getStatus() == UserAccountStatus.ACTIVE)
                .filter(account -> userPersonLinkRepository.findByUserAccountId(account.getId()).stream()
                        .noneMatch(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED))
                .toList();
    }

    @Transactional
    public void linkAccountToPerson(Long userAccountId, Long personId, String actorUsername) {
        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + userAccountId));
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + personId));

        UserPersonLink link = new UserPersonLink();
        link.setUserAccount(account);
        link.setPerson(person);
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        link.setVerifiedAt(LocalDateTime.now());
        userPersonLinkRepository.save(link);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_LINKED, AuditLogService.ENTITY_USER_ACCOUNT, userAccountId,
                "Linked account " + account.getEmail() + " to person " + personDisplay.englishFullName(person), actorUsername);
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
