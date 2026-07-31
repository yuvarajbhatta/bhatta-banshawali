package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.RelationshipType;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.web.PersonDisplayHelper;
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
    private final RelationshipService relationshipService;
    private final UserPersonLinkService userPersonLinkService;
    private final PersonDisplayHelper personDisplay;
    private final AuditLogService auditLogService;

    public VerificationReviewService(VerificationRequestRepository verificationRequestRepository,
                                     UserAccountRepository userAccountRepository,
                                     RoleRepository roleRepository,
                                     PersonRepository personRepository,
                                     RelationshipService relationshipService,
                                     UserPersonLinkService userPersonLinkService,
                                     PersonDisplayHelper personDisplay,
                                     AuditLogService auditLogService) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.personRepository = personRepository;
        this.relationshipService = relationshipService;
        this.userPersonLinkService = userPersonLinkService;
        this.personDisplay = personDisplay;
        this.auditLogService = auditLogService;
    }

    /**
     * @param linkedPersonId the existing Person record the admin has confirmed this
     *                        applicant is, from the candidates shown in the review UI
     *                        (docs/04-data-model.md UserPersonLink) -- null when no
     *                        candidate matched (the applicant is a genuinely new person,
     *                        or the admin couldn't confirm one). Approval still proceeds
     *                        either way; only the link is skipped.
     * @param createAsChildOfFatherId the existing Person (the father) to create a brand-new
     *                        Person for this applicant under, as a FATHER relationship --
     *                        for applicants who don't yet exist in the tree but whose father
     *                        does. Mutually exclusive with linkedPersonId.
     * @throws IllegalArgumentException if both linkedPersonId and createAsChildOfFatherId are
     *          provided, or if UserPersonLinkService's guard rejects the resulting link (see
     *          its javadoc)
     */
    @Transactional
    public void approve(Long verificationRequestId, String reviewerUsername, String decisionNote,
                        Long linkedPersonId, Long createAsChildOfFatherId) {
        if (linkedPersonId != null && createAsChildOfFatherId != null) {
            throw new IllegalArgumentException("linkedPersonId and createAsChildOfFatherId cannot both be provided.");
        }

        VerificationRequest request = getOrThrow(verificationRequestId);
        markReviewed(request, VerificationStatus.APPROVED, reviewerUsername, decisionNote);

        UserAccount account = request.getUserAccount();
        account.setStatus(UserAccountStatus.ACTIVE);
        roleRepository.findByName(VERIFIED_MEMBER_ROLE).ifPresent(role -> account.getRoles().add(role));
        userAccountRepository.save(account);

        Person personToLink = null;
        if (createAsChildOfFatherId != null) {
            Person father = personRepository.findById(createAsChildOfFatherId)
                    .orElseThrow(() -> new RuntimeException("Person not found with id: " + createAsChildOfFatherId));
            personToLink = createNewPersonAsChildOf(father, request, reviewerUsername);
        } else if (linkedPersonId != null) {
            personToLink = personRepository.findById(linkedPersonId)
                    .orElseThrow(() -> new RuntimeException("Person not found with id: " + linkedPersonId));
        }

        if (personToLink != null) {
            userPersonLinkService.createVerifiedLink(account, personToLink);
        }

        auditLogService.record(AuditLogService.ACTION_SIGNUP_APPROVED, AuditLogService.ENTITY_VERIFICATION_REQUEST,
                verificationRequestId, "Approved signup for " + request.getSubmittedFullName(), reviewerUsername);
    }

    /**
     * Creates a brand-new Person from the applicant's submitted signup info, links it as a
     * FATHER relationship to the given father (auto-creating the reciprocal CHILD edge and
     * any spouse auto-link -- see RelationshipService.saveRelationshipWithAutoLinks), and
     * derives generationNumber from the father's own (null-safe: left null if the father has
     * none recorded -- generationNumber is a plain nullable field everywhere else, and this
     * is the first place in the codebase deriving it from a relationship rather than an
     * explicit admin value).
     */
    private Person createNewPersonAsChildOf(Person father, VerificationRequest request, String reviewerUsername) {
        Person newPerson = new Person();
        FullNameParser.applyTo(newPerson, request.getSubmittedFullName());
        newPerson.setBirthDate(request.getSubmittedDobAd());
        newPerson.setGenerationNumber(father.getGenerationNumber() != null ? father.getGenerationNumber() + 1 : null);
        newPerson = personRepository.save(newPerson);

        relationshipService.saveRelationshipWithAutoLinks(newPerson, father, RelationshipType.FATHER);

        auditLogService.record(AuditLogService.ACTION_PERSON_CREATED, AuditLogService.ENTITY_PERSON, newPerson.getId(),
                "Created new person for signup \"" + request.getSubmittedFullName() + "\" as child of "
                        + personDisplay.englishFullName(father), reviewerUsername);

        return newPerson;
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
