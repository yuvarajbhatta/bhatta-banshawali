package com.familytree.services;

import com.familytree.dto.AdminAccessRequestDto;
import com.familytree.dto.MyAdminAccessRequestStatusDto;
import com.familytree.entity.AdminAccessRequest;
import com.familytree.entity.AdminAccessRequestStatus;
import com.familytree.entity.Person;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.AdminAccessRequestRepository;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * A member asking an existing admin to grant them the ADMINISTRATOR
 * role, reviewed the same way signup/correction requests are (docs/08
 * Phase 6): an existing admin sees the request and approves or denies
 * it; approval actually grants the role.
 */
@Service
public class AdminAccessRequestService {

    private static final String ADMINISTRATOR_ROLE = "ADMINISTRATOR";
    private static final Set<String> ADMIN_ROLE_NAMES = Set.of("ADMINISTRATOR", "SUPER_ADMINISTRATOR");

    private final AdminAccessRequestRepository adminAccessRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final PersonDisplayHelper personDisplay;

    public AdminAccessRequestService(AdminAccessRequestRepository adminAccessRequestRepository,
                                     UserAccountRepository userAccountRepository,
                                     UserPersonLinkRepository userPersonLinkRepository,
                                     RoleRepository roleRepository,
                                     AuditLogService auditLogService,
                                     PersonDisplayHelper personDisplay) {
        this.adminAccessRequestRepository = adminAccessRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.personDisplay = personDisplay;
    }

    /**
     * @throws IllegalArgumentException if the account already has admin access, or
     *          already has a pending request
     */
    @Transactional
    public void request(Long userAccountId) {
        UserAccount account = getAccountOrThrow(userAccountId);
        if (isAlreadyAdmin(account)) {
            throw new IllegalArgumentException("You already have admin access.");
        }
        if (hasPendingRequest(userAccountId)) {
            throw new IllegalArgumentException("You already have a pending request.");
        }

        AdminAccessRequest request = new AdminAccessRequest();
        request.setUserAccount(account);
        request.setStatus(AdminAccessRequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        adminAccessRequestRepository.save(request);

        auditLogService.record(AuditLogService.ACTION_ADMIN_ACCESS_REQUESTED, AuditLogService.ENTITY_USER_ACCOUNT,
                userAccountId, account.getEmail() + " requested admin access", account.getEmail());
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
        return hasPendingRequest(userAccountId)
                ? MyAdminAccessRequestStatusDto.pending()
                : MyAdminAccessRequestStatusDto.none();
    }

    private boolean hasPendingRequest(Long userAccountId) {
        return adminAccessRequestRepository.findByUserAccountId(userAccountId).stream()
                .anyMatch(request -> request.getStatus() == AdminAccessRequestStatus.PENDING);
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
