package com.familytree.services;

import com.familytree.dto.AdminAccountSignupInfoUpdateDto;
import com.familytree.dto.AdminUserAccountDto;
import com.familytree.entity.Person;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.repository.AdminAccessRequestRepository;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserAccountTokenRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Admin management of every UserAccount, combining what used to be
 * split across AdminAccountLinkApiController (link, unlinked accounts
 * only) and this service (disable/enable) into one "Manage User
 * Accounts" surface: link/unlink to a Person, correct the applicant's
 * submitted signup info, revoke (disable) or restore (enable) login
 * access, or permanently delete the account.
 */
@Service
public class UserAccountAdminService {

    private static final Set<String> ADMIN_ROLE_NAMES = Set.of("ADMINISTRATOR", "SUPER_ADMINISTRATOR");

    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final PersonCorrectionRequestRepository personCorrectionRequestRepository;
    private final AdminAccessRequestRepository adminAccessRequestRepository;
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;
    private final PersonDisplayHelper personDisplay;
    private final UserPersonLinkService userPersonLinkService;
    private final UserAccountTokenRepository userAccountTokenRepository;

    public UserAccountAdminService(UserAccountRepository userAccountRepository,
                                   UserPersonLinkRepository userPersonLinkRepository,
                                   VerificationRequestRepository verificationRequestRepository,
                                   PersonCorrectionRequestRepository personCorrectionRequestRepository,
                                   AdminAccessRequestRepository adminAccessRequestRepository,
                                   PersonRepository personRepository,
                                   AuditLogService auditLogService,
                                   PersonDisplayHelper personDisplay,
                                   UserPersonLinkService userPersonLinkService,
                                   UserAccountTokenRepository userAccountTokenRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.personCorrectionRequestRepository = personCorrectionRequestRepository;
        this.adminAccessRequestRepository = adminAccessRequestRepository;
        this.personRepository = personRepository;
        this.auditLogService = auditLogService;
        this.personDisplay = personDisplay;
        this.userPersonLinkService = userPersonLinkService;
        this.userAccountTokenRepository = userAccountTokenRepository;
    }

    public List<AdminUserAccountDto> listAll() {
        return userAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    /**
     * @throws IllegalArgumentException if the acting admin tries to disable their own
     *          account -- a small family site typically has very few admins, and a
     *          self-lockout has no in-app recovery path.
     */
    @Transactional
    public void disable(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        if (account.getEmail().equalsIgnoreCase(actorUsername)) {
            throw new IllegalArgumentException("You cannot disable your own account.");
        }

        account.setStatus(UserAccountStatus.DISABLED);
        userAccountRepository.save(account);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_DISABLED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Disabled account " + account.getEmail(), actorUsername);
    }

    @Transactional
    public void enable(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(account);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_ENABLED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Re-enabled account " + account.getEmail(), actorUsername);
    }

    /**
     * @throws IllegalArgumentException if the account already has a VERIFIED link (unlink
     *          it first), or the target Person is already VERIFIED-linked to a different
     *          account -- otherwise picking the wrong same-named candidate (e.g. one of
     *          several "Bhojraj Bhatta" records) succeeds silently with no way to notice.
     */
    @Transactional
    public void link(Long userAccountId, Long personId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + personId));

        userPersonLinkService.createVerifiedLink(account, person);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_LINKED, AuditLogService.ENTITY_USER_ACCOUNT, userAccountId,
                "Linked account " + account.getEmail() + " to person " + personDisplay.englishFullName(person), actorUsername);
    }

    /** Removes the account's VERIFIED Person link, e.g. to re-link it to the correct person. */
    @Transactional
    public void unlink(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        List<UserPersonLink> verifiedLinks = userPersonLinkRepository.findByUserAccountId(userAccountId).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .toList();

        if (verifiedLinks.isEmpty()) {
            throw new IllegalArgumentException("This account isn't linked to a person.");
        }

        userPersonLinkRepository.deleteAll(verifiedLinks);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_UNLINKED, AuditLogService.ENTITY_USER_ACCOUNT, userAccountId,
                "Unlinked account " + account.getEmail(), actorUsername);
    }

    /**
     * Corrects the applicant's submitted identity info on their most recent
     * VerificationRequest -- e.g. a fake name at signup, or disambiguating
     * from another Person with the same name before linking.
     *
     * @throws IllegalArgumentException if the account has no signup record to edit
     */
    @Transactional
    public void updateSignupInfo(Long userAccountId, AdminAccountSignupInfoUpdateDto request, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        VerificationRequest mostRecent = mostRecentVerificationRequest(userAccountId)
                .orElseThrow(() -> new IllegalArgumentException("This account has no signup record to edit."));

        mostRecent.setSubmittedFullName(request.getFullName());
        mostRecent.setSubmittedFatherName(request.getFatherName());
        mostRecent.setMotherName(request.getMotherName());
        mostRecent.setSubmittedGrandfatherName(request.getGrandfatherName());
        mostRecent.setSubmittedDobAd(request.getDobAd());
        verificationRequestRepository.save(mostRecent);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_SIGNUP_INFO_EDITED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Edited submitted signup info for " + account.getEmail(), actorUsername);
    }

    /**
     * Copies the applicant's submitted name and date of birth onto their
     * linked Person record -- e.g. the signup form has a corrected DOB or
     * spelling the existing Person record doesn't. Deliberately limited to
     * fields Person actually stores directly: father's/mother's/
     * grandfather's names have no equivalent Person field (parentage is
     * represented as Relationship edges, not strings) and are never
     * auto-wired into the family graph from free text -- that stays a
     * manual, admin-confirmed step via the Relationships tool, the same
     * as every other edge in the tree.
     *
     * @throws IllegalArgumentException if the account isn't linked to a person, or has
     *          no signup record to apply
     */
    @Transactional
    public void applySignupInfoToPerson(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        Person person = userPersonLinkRepository.findByUserAccountId(userAccountId).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .map(UserPersonLink::getPerson)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("This account isn't linked to a person."));
        VerificationRequest mostRecent = mostRecentVerificationRequest(userAccountId)
                .orElseThrow(() -> new IllegalArgumentException("This account has no signup info to apply."));

        FullNameParser.applyTo(person, mostRecent.getSubmittedFullName());
        if (mostRecent.getSubmittedDobAd() != null) {
            person.setBirthDate(mostRecent.getSubmittedDobAd());
        }
        personRepository.save(person);

        auditLogService.record(AuditLogService.ACTION_PERSON_UPDATED, AuditLogService.ENTITY_PERSON, person.getId(),
                "Applied submitted signup info from " + account.getEmail() + " to "
                        + personDisplay.englishFullName(person), actorUsername);
    }

    /**
     * @throws IllegalArgumentException if the account has no admin role to revoke, or
     *          the acting admin tries to revoke their own access
     */
    @Transactional
    public void revokeAdminAccess(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        if (account.getEmail().equalsIgnoreCase(actorUsername)) {
            throw new IllegalArgumentException("You cannot revoke your own admin access.");
        }
        boolean removed = account.getRoles().removeIf(role -> ADMIN_ROLE_NAMES.contains(role.getName()));
        if (!removed) {
            throw new IllegalArgumentException("This account doesn't have admin access.");
        }
        userAccountRepository.save(account);

        auditLogService.record(AuditLogService.ACTION_ADMIN_ACCESS_REVOKED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Revoked admin access for " + account.getEmail(), actorUsername);
    }

    /**
     * Permanently wipes the account -- their own signup submissions,
     * person link, correction submissions, admin-access requests, and
     * password-reset/email-verification tokens -- so the email address
     * is free for a fresh signup. Backward references where this account
     * acted as a reviewer/verifier of someone else's request are nulled
     * out (those columns are nullable and the reviewer's username is
     * already preserved separately as a string), not deleted, so other
     * people's review history survives.
     *
     * @throws IllegalArgumentException if the acting admin tries to delete their own
     *          account, or the target account currently has admin access (revoke it
     *          first via {@link #revokeAdminAccess}, then delete) -- deleting an admin
     *          account is a deliberately harder, two-step action, not one click
     */
    @Transactional
    public void delete(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        if (account.getEmail().equalsIgnoreCase(actorUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        if (isAdmin(account)) {
            throw new IllegalArgumentException("This account has admin access. Revoke admin access before deleting it.");
        }
        String email = account.getEmail();

        verificationRequestRepository.findByReviewedById(userAccountId).forEach(request -> request.setReviewedBy(null));
        userPersonLinkRepository.findByVerifiedById(userAccountId).forEach(link -> link.setVerifiedBy(null));

        userPersonLinkRepository.deleteAll(userPersonLinkRepository.findByUserAccountId(userAccountId));
        personCorrectionRequestRepository.deleteAll(personCorrectionRequestRepository.findBySubmittedById(userAccountId));
        adminAccessRequestRepository.deleteAll(adminAccessRequestRepository.findByUserAccountId(userAccountId));
        verificationRequestRepository.deleteAll(verificationRequestRepository.findByUserAccountId(userAccountId));
        userAccountTokenRepository.deleteAll(userAccountTokenRepository.findByUserAccountId(userAccountId));
        userAccountRepository.delete(account);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_DELETED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Deleted account " + email, actorUsername);
    }

    private boolean isAdmin(UserAccount account) {
        return account.getRoles().stream().map(Role::getName).anyMatch(ADMIN_ROLE_NAMES::contains);
    }

    private Optional<VerificationRequest> mostRecentVerificationRequest(Long userAccountId) {
        return verificationRequestRepository.findByUserAccountId(userAccountId).stream()
                .max(Comparator.comparing(VerificationRequest::getCreatedAt));
    }

    private AdminUserAccountDto toDto(UserAccount account) {
        Person linkedPerson = userPersonLinkRepository.findByUserAccountId(account.getId()).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .map(UserPersonLink::getPerson)
                .findFirst()
                .orElse(null);

        VerificationRequest mostRecent = mostRecentVerificationRequest(account.getId()).orElse(null);

        return new AdminUserAccountDto(
                account.getId(),
                account.getEmail(),
                account.getStatus(),
                account.getPreferredLanguage(),
                account.getCreatedAt(),
                account.getLastLoginAt(),
                isAdmin(account),
                linkedPerson != null ? linkedPerson.getId() : null,
                linkedPerson != null ? personDisplay.englishFullName(linkedPerson) : null,
                mostRecent != null ? mostRecent.getSubmittedFullName() : null,
                mostRecent != null ? mostRecent.getSubmittedFatherName() : null,
                mostRecent != null ? mostRecent.getMotherName() : null,
                mostRecent != null ? mostRecent.getSubmittedGrandfatherName() : null,
                mostRecent != null ? mostRecent.getSubmittedDobAd() : null
        );
    }

    private UserAccount getOrThrow(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));
    }
}
