package com.familytree.services;

import com.familytree.dto.AdminAccountSignupInfoUpdateDto;
import com.familytree.dto.AdminUserAccountDto;
import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
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

    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final PersonCorrectionRequestRepository personCorrectionRequestRepository;
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;
    private final PersonDisplayHelper personDisplay;

    public UserAccountAdminService(UserAccountRepository userAccountRepository,
                                   UserPersonLinkRepository userPersonLinkRepository,
                                   VerificationRequestRepository verificationRequestRepository,
                                   PersonCorrectionRequestRepository personCorrectionRequestRepository,
                                   PersonRepository personRepository,
                                   AuditLogService auditLogService,
                                   PersonDisplayHelper personDisplay) {
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.personCorrectionRequestRepository = personCorrectionRequestRepository;
        this.personRepository = personRepository;
        this.auditLogService = auditLogService;
        this.personDisplay = personDisplay;
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

    @Transactional
    public void link(Long userAccountId, Long personId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + personId));

        UserPersonLink link = new UserPersonLink();
        link.setUserAccount(account);
        link.setPerson(person);
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        link.setVerifiedAt(LocalDateTime.now());
        userPersonLinkRepository.save(link);

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
        mostRecent.setSubmittedGrandfatherName(request.getGrandfatherName());
        mostRecent.setSubmittedDobAd(request.getDobAd());
        verificationRequestRepository.save(mostRecent);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_SIGNUP_INFO_EDITED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Edited submitted signup info for " + account.getEmail(), actorUsername);
    }

    /**
     * Permanently wipes the account -- their own signup submissions,
     * person link, and correction submissions -- so the email address is
     * free for a fresh signup. Backward references where this account
     * acted as a reviewer/verifier of someone else's request are nulled
     * out (those columns are nullable and the reviewer's username is
     * already preserved separately as a string), not deleted, so other
     * people's review history survives.
     *
     * @throws IllegalArgumentException if the acting admin tries to delete their own account
     */
    @Transactional
    public void delete(Long userAccountId, String actorUsername) {
        UserAccount account = getOrThrow(userAccountId);
        if (account.getEmail().equalsIgnoreCase(actorUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        String email = account.getEmail();

        verificationRequestRepository.findByReviewedById(userAccountId).forEach(request -> request.setReviewedBy(null));
        userPersonLinkRepository.findByVerifiedById(userAccountId).forEach(link -> link.setVerifiedBy(null));

        userPersonLinkRepository.deleteAll(userPersonLinkRepository.findByUserAccountId(userAccountId));
        personCorrectionRequestRepository.deleteAll(personCorrectionRequestRepository.findBySubmittedById(userAccountId));
        verificationRequestRepository.deleteAll(verificationRequestRepository.findByUserAccountId(userAccountId));
        userAccountRepository.delete(account);

        auditLogService.record(AuditLogService.ACTION_ACCOUNT_DELETED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, "Deleted account " + email, actorUsername);
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
                linkedPerson != null ? linkedPerson.getId() : null,
                linkedPerson != null ? personDisplay.englishFullName(linkedPerson) : null,
                mostRecent != null ? mostRecent.getSubmittedFullName() : null,
                mostRecent != null ? mostRecent.getSubmittedFatherName() : null,
                mostRecent != null ? mostRecent.getSubmittedGrandfatherName() : null,
                mostRecent != null ? mostRecent.getSubmittedDobAd() : null
        );
    }

    private UserAccount getOrThrow(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));
    }
}
