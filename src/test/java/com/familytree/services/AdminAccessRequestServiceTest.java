package com.familytree.services;

import com.familytree.dto.AdminAccessRequestDto;
import com.familytree.dto.MyAdminAccessRequestStatusDto;
import com.familytree.entity.AdminAccessRequest;
import com.familytree.entity.AdminAccessRequestStatus;
import com.familytree.entity.OtpPurpose;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AdminAccessRequestRepository;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.email.EmailService;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccessRequestServiceTest {

    @Mock
    private AdminAccessRequestRepository adminAccessRequestRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PersonDisplayHelper personDisplay;

    @InjectMocks
    private AdminAccessRequestService service;

    private UserAccount account(long id, String email) {
        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", id);
        account.setEmail(email);
        account.setRoles(new HashSet<>());
        return account;
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    @Test
    void requestCreatesAwaitingOtpRequestAndEmailsACode() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(adminAccessRequestRepository.findByUserAccountId(6L)).thenReturn(List.of());
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.empty());
        when(otpService.generate(account, OtpPurpose.ADMIN_ACCESS_REQUEST)).thenReturn("123456");

        service.request(6L);

        ArgumentCaptor<AdminAccessRequest> captor = ArgumentCaptor.forClass(AdminAccessRequest.class);
        verify(adminAccessRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAccount()).isEqualTo(account);
        assertThat(captor.getValue().getStatus()).isEqualTo(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);

        verify(emailService).sendAdminAccessOtpEmail("member@example.com", "123456", account.getPreferredLanguage());
        verify(auditLogService).record(AuditLogService.ACTION_ADMIN_ACCESS_REQUESTED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "member@example.com requested admin access", "member@example.com");
    }

    @Test
    void requestingAgainWhileAwaitingOtpReissuesTheExistingRequestRatherThanCreatingANewOne() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(adminAccessRequestRepository.findByUserAccountId(6L)).thenReturn(List.of());
        AdminAccessRequest existingAwaitingOtp = new AdminAccessRequest();
        ReflectionTestUtils.setField(existingAwaitingOtp, "id", 10L);
        existingAwaitingOtp.setUserAccount(account);
        existingAwaitingOtp.setStatus(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.of(existingAwaitingOtp));
        when(otpService.generate(account, OtpPurpose.ADMIN_ACCESS_REQUEST)).thenReturn("654321");

        service.request(6L);

        ArgumentCaptor<AdminAccessRequest> captor = ArgumentCaptor.forClass(AdminAccessRequest.class);
        verify(adminAccessRequestRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existingAwaitingOtp);
        verify(emailService).sendAdminAccessOtpEmail("member@example.com", "654321", account.getPreferredLanguage());
    }

    @Test
    void requestRejectsWhenAlreadyAdmin() {
        UserAccount account = account(6L, "admin@example.com");
        account.setRoles(Set.of(role("ADMINISTRATOR")));
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.request(6L)).isInstanceOf(IllegalArgumentException.class);

        verify(adminAccessRequestRepository, never()).save(any());
    }

    @Test
    void requestRejectsWhenAlreadyPending() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        AdminAccessRequest existing = new AdminAccessRequest();
        existing.setStatus(AdminAccessRequestStatus.PENDING);
        when(adminAccessRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.request(6L)).isInstanceOf(IllegalArgumentException.class);

        verify(adminAccessRequestRepository, never()).save(any());
    }

    @Test
    void confirmRequestMovesAnAwaitingOtpRequestToPending() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        AdminAccessRequest request = new AdminAccessRequest();
        request.setUserAccount(account);
        request.setStatus(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.of(request));
        when(otpService.verify(account, OtpPurpose.ADMIN_ACCESS_REQUEST, "123456")).thenReturn(OtpVerifyResult.OK);

        service.confirmRequest(6L, "123456");

        assertThat(request.getStatus()).isEqualTo(AdminAccessRequestStatus.PENDING);
        verify(adminAccessRequestRepository).save(request);
    }

    @Test
    void confirmRequestThrowsForAnInvalidCode() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        AdminAccessRequest request = new AdminAccessRequest();
        request.setUserAccount(account);
        request.setStatus(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.of(request));
        when(otpService.verify(account, OtpPurpose.ADMIN_ACCESS_REQUEST, "000000")).thenReturn(OtpVerifyResult.INVALID_CODE);

        assertThatThrownBy(() -> service.confirmRequest(6L, "000000"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        assertThat(request.getStatus()).isEqualTo(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
    }

    @Test
    void confirmRequestThrowsWhenNoRequestIsAwaitingConfirmation() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmRequest(6L, "123456"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(otpService, never()).verify(any(), any(), any());
    }

    @Test
    void approveGrantsAdministratorRoleAndLogsIt() {
        UserAccount account = account(6L, "member@example.com");
        AdminAccessRequest request = new AdminAccessRequest();
        ReflectionTestUtils.setField(request, "id", 10L);
        request.setUserAccount(account);
        request.setStatus(AdminAccessRequestStatus.PENDING);
        when(adminAccessRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        Role administratorRole = role("ADMINISTRATOR");
        when(roleRepository.findByName("ADMINISTRATOR")).thenReturn(Optional.of(administratorRole));

        service.approve(10L, "admin@example.com");

        assertThat(account.getRoles()).contains(administratorRole);
        assertThat(request.getStatus()).isEqualTo(AdminAccessRequestStatus.APPROVED);
        assertThat(request.getReviewedByUsername()).isEqualTo("admin@example.com");
        verify(userAccountRepository).save(account);
        verify(auditLogService).record(AuditLogService.ACTION_ADMIN_ACCESS_APPROVED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "Granted admin access to member@example.com", "admin@example.com");
    }

    @Test
    void approveThrowsWhenAlreadyReviewed() {
        AdminAccessRequest request = new AdminAccessRequest();
        request.setUserAccount(account(6L, "member@example.com"));
        request.setStatus(AdminAccessRequestStatus.DENIED);
        when(adminAccessRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(10L, "admin@example.com")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void denyMarksDeniedWithoutGrantingRoleAndLogsIt() {
        UserAccount account = account(6L, "member@example.com");
        AdminAccessRequest request = new AdminAccessRequest();
        request.setUserAccount(account);
        request.setStatus(AdminAccessRequestStatus.PENDING);
        when(adminAccessRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        service.deny(10L, "admin@example.com");

        assertThat(account.getRoles()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(AdminAccessRequestStatus.DENIED);
        verify(userAccountRepository, never()).save(any());
        verify(auditLogService).record(AuditLogService.ACTION_ADMIN_ACCESS_DENIED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "Denied admin access for member@example.com", "admin@example.com");
    }

    @Test
    void findPendingMapsToDtos() {
        UserAccount account = account(6L, "member@example.com");
        AdminAccessRequest request = new AdminAccessRequest();
        ReflectionTestUtils.setField(request, "id", 10L);
        request.setUserAccount(account);
        when(adminAccessRequestRepository.findAllByStatusOrderByRequestedAtAsc(AdminAccessRequestStatus.PENDING))
                .thenReturn(List.of(request));
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of());

        List<AdminAccessRequestDto> result = service.findPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("member@example.com");
        assertThat(result.get(0).userAccountId()).isEqualTo(6L);
    }

    @Test
    void myStatusReportsAlreadyAdmin() {
        UserAccount account = account(6L, "admin@example.com");
        account.setRoles(Set.of(role("ADMINISTRATOR")));
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        assertThat(service.myStatus(6L)).isEqualTo(MyAdminAccessRequestStatusDto.alreadyAdmin());
    }

    @Test
    void myStatusReportsAwaitingOtpWhenARequestIsAwaitingConfirmation() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        AdminAccessRequest request = new AdminAccessRequest();
        request.setStatus(AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION);
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.of(request));

        assertThat(service.myStatus(6L)).isEqualTo(MyAdminAccessRequestStatusDto.awaitingOtp());
    }

    @Test
    void myStatusReportsPendingWhenARequestIsPending() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.empty());
        AdminAccessRequest request = new AdminAccessRequest();
        request.setStatus(AdminAccessRequestStatus.PENDING);
        when(adminAccessRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(request));

        assertThat(service.myStatus(6L)).isEqualTo(MyAdminAccessRequestStatusDto.pending());
    }

    @Test
    void myStatusReportsNoneWhenNothingOnFile() {
        UserAccount account = account(6L, "member@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(adminAccessRequestRepository.findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
                6L, AdminAccessRequestStatus.AWAITING_OTP_CONFIRMATION)).thenReturn(Optional.empty());
        when(adminAccessRequestRepository.findByUserAccountId(6L)).thenReturn(List.of());

        assertThat(service.myStatus(6L)).isEqualTo(MyAdminAccessRequestStatusDto.none());
    }

    @Test
    void approveThrowsWhenRequestNotFound() {
        when(adminAccessRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(999L, "admin@example.com")).isInstanceOf(ResponseStatusException.class);
    }
}
