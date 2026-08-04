package com.familytree.services;

import com.familytree.dto.AdminAccessRequestDto;
import com.familytree.dto.MyAdminAccessRequestStatusDto;
import com.familytree.entity.AdminAccessRequest;
import com.familytree.entity.AdminAccessRequestStatus;
import com.familytree.entity.OtpPurpose;
import com.familytree.entity.Person;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.AdminAccessRequestRepository;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.email.EmailService;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A member asking an existing admin to grant them the ADMINISTRATOR
 * role, reviewed the same way signup/correction requests are (docs/08
 * Phase 6): the requester must first confirm an OTP emailed to their own
 * address -- filters out mistaken/spam requests before any admin has to
 * look -- and only once confirmed does the request become visible in the
 * review queue; an existing admin then approves or denies it, and
 * approval actually grants the role. Calling {@link #request} again while
 * a request is still AWAITING_OTP_CONFIRMATION (e.g. the first code
 * expired before the user entered it) reuses that same row and just
 * re-emails a fresh code, rather than erroring or piling up duplicate
 * rows -- only a request that's already reached PENDING blocks a
 * re-request.
 */
@Service
public class AdminAccessRequestService {

    private static final String ADMINISTRATOR_ROLE = "ADMINISTRATOR";
    private static final Set<String> ADMIN_ROLE_NAMES = Set.of("ADMINISTRATOR", "SUPER_ADMINISTRATOR");

    private final AdminAccessRequestRepository adminAccessRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final RoleRepository roleRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final PersonDisplayHelper personDisplay;

    public AdminAccessRequestService(AdminAccessRequestRepository adminAccessRequestRepository,
                                     UserAccountRepository userAccountRepository,
                                     UserPersonLinkRepository userPersonLinkRepository,
                                     RoleRepository roleRepository,
                                     OtpService otpService,
                                     EmailService emailService,
                                     AuditLogService auditLogService,
                                     PersonDisplayHelper personDisplay) {
        this.adminAccessRequestRepository = adminAccessRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.roleRepository = roleRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.personDisplay = personDisplay;
    }

    /**
     * Creates (or reuses an existing not-yet-confirmed) request and
     * emails a fresh OTP the requester must confirm before it becomes
     * visible to admins -- see {@link #confirmRequest}.
     *
     * @throws IllegalArgumentException if the account already has admin access, or
     *          already has a request pending admin review (an
     *          AWAITING_OTP_CONFIRMATION request, by contrast, is silently
     *          reused/reissued below -- see the class javadoc)
     */
    @Transactional
    public void request(Long userAccountId) {
        UserAccount account = getAccountOrThrow(userAccountId);
        if (isAlreadyAdmin(account)) {
            throw new IllegalArgumentException("You already have admin access.");
        }
        if (hasPendingReview(userAccountId)) {
            throw new IllegalArgumentException("You already have a pending request.");
        }

        AdminAccessRequest request = findAwaitingOtp(userAccountId).orElseGet(() -> {
            AdminAccessRequest fresh = new AdminAccessRequest();
            fresh.setUserAccount(account);
            fresh.setStatus(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
            fresh.setRequestedAt(LocalDateTime.now());
            return fresh;
        });
        adminAccessRequestRepository.save(request);

        String code = otpService.generate(account, OtpPurpose.ADMIN_ACCESS_REQUEST);
        emailService.sendAdminAccessOtpEmail(account.getEmail(), code, account.getPreferredLanguage());

        auditLogService.record(AuditLogService.ACTION_ADMIN_ACCESS_REQUESTED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, account.getEmail() + " requested admin access", account.getEmail());
    }

    /**
     * Confirms the OTP emailed by {@link #request}, moving the request
     * from AWAITING_OTP_CONFIRMATION to PENDING so it becomes visible in
     * the admin review queue.
     *
     * @throws InvalidOrExpiredTokenException if the code doesn't match,
     *          expired, was guessed wrong too many times, or there's no
     *          request awaiting confirmation for this account
     */
    @Transactional
    public void confirmRequest(Long userAccountId, String code) {
        UserAccount account = getAccountOrThrow(userAccountId);
        AdminAccessRequest request = findAwaitingOtp(userAccountId)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("No admin access request is awaiting confirmation."));

        OtpVerifyResult result = otpService.verify(account, OtpPurpose.ADMIN_ACCESS_REQUEST, code);
        if (result != OtpVerifyResult.OK) {
            throw new InvalidOrExpiredTokenException(messageFor(result));
        }

        request.setStatus(AdminAccessRequestStatus.PENDING);
        adminAccessRequestRepository.save(request);
    }

    @Transactional
    public void approve(Long requestId, String reviewerUsername) {
        AdminAccessRequest request = getRequestOrThrow(requestId);
        requirePending(request);

        UserAccount account = request.getUserAccount();
        roleRepository.findByName(ADMINISTRATOR_ROLE).ifPresent(role -> account.getRoles().add(role));
        userAccountRepository.save(account);

        markReviewed(request, AdminAccessRequestStatus.APPROVED, reviewerUsername);

        auditLogService.record(AuditLogService.ACTION_ADMIN_ACCESS_APPROVED, AuditLogService.ENTITY_USER_ACCOUNT,
                account.getId(), "Granted admin access to " + account.getEmail(), reviewerUsername);
    }

    @Transactional
    public void deny(Long requestId, String reviewerUsername) {
        AdminAccessRequest request = getRequestOrThrow(requestId);
        requirePending(request);

        markReviewed(request, AdminAccessRequestStatus.DENIED, reviewerUsername);

        auditLogService.record(AuditLogService.ACTION_ADMIN_ACCESS_DENIED, AuditLogService.ENTITY_USER_ACCOUNT,
                request.getUserAccount().getId(), "Denied admin access for " + request.getUserAccount().getEmail(),
                reviewerUsername);
    }

    public List<AdminAccessRequestDto> findPending() {
        return adminAccessRequestRepository.findAllByStatusOrderByRequestedAtAsc(AdminAccessRequestStatus.PENDING).stream()
                .map(this::toDto)
                .toList();
    }

    public int pendingCount() {
        return adminAccessRequestRepository.findAllByStatusOrderByRequestedAtAsc(AdminAccessRequestStatus.PENDING).size();
    }

    public MyAdminAccessRequestStatusDto myStatus(Long userAccountId) {
        UserAccount account = getAccountOrThrow(userAccountId);
        if (isAlreadyAdmin(account)) {
            return MyAdminAccessRequestStatusDto.alreadyAdmin();
        }
        if (findAwaitingOtp(userAccountId).isPresent()) {
            return MyAdminAccessRequestStatusDto.awaitingOtp();
        }
        return hasPendingReview(userAccountId)
                ? MyAdminAccessRequestStatusDto.pending()
                : MyAdminAccessRequestStatusDto.none();
    }

    private boolean hasPendingReview(Long userAccountId) {
        return adminAccessRequestRepository.findByUserAccountId(userAccountId).stream()
                .anyMatch(request -> request.getStatus() == AdminAccessRequestStatus.PENDING);
    }

    private Optional<AdminAccessRequest> findAwaitingOtp(Long userAccountId) {
        return adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                userAccountId, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
    }

    private boolean isAlreadyAdmin(UserAccount account) {
        return account.getRoles().stream().map(Role::getName).anyMatch(ADMIN_ROLE_NAMES::contains);
    }

    private void requirePending(AdminAccessRequest request) {
        if (request.getStatus() != AdminAccessRequestStatus.PENDING) {
            throw new IllegalArgumentException("This request has already been reviewed.");
        }
    }

    private void markReviewed(AdminAccessRequest request, AdminAccessRequestStatus status, String reviewerUsername) {
        request.setStatus(status);
        request.setReviewedByUsername(reviewerUsername);
        request.setReviewedAt(LocalDateTime.now());
        adminAccessRequestRepository.save(request);
    }

    private String messageFor(OtpVerifyResult result) {
        return switch (result) {
            case EXPIRED -> "This code has expired. Request a new one.";
            case TOO_MANY_ATTEMPTS -> "Too many incorrect attempts. Request a new code.";
            case NOT_FOUND -> "No confirmation code is pending. Request a new one.";
            default -> "This confirmation code is invalid.";
        };
    }

    private AdminAccessRequestDto toDto(AdminAccessRequest request) {
        UserAccount account = request.getUserAccount();
        Person linkedPerson = userPersonLinkRepository.findByUserAccountId(account.getId()).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .map(UserPersonLink::getPerson)
                .findFirst()
                .orElse(null);

        return new AdminAccessRequestDto(
                request.getId(),
                account.getId(),
                account.getEmail(),
                linkedPerson != null ? personDisplay.englishFullName(linkedPerson) : null,
                request.getRequestedAt()
        );
    }

    private UserAccount getAccountOrThrow(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));
    }

    private AdminAccessRequest getRequestOrThrow(Long id) {
        return adminAccessRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found with id: " + id));
    }
}
