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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountAdminServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private PersonCorrectionRequestRepository personCorrectionRequestRepository;

    @Mock
    private AdminAccessRequestRepository adminAccessRequestRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PersonDisplayHelper personDisplay;

    @Mock
    private UserPersonLinkService userPersonLinkService;

    @Mock
    private UserAccountTokenRepository userAccountTokenRepository;

    @InjectMocks
    private UserAccountAdminService service;

    private UserAccount account(long id, String email, UserAccountStatus status) {
        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", id);
        account.setEmail(email);
        account.setStatus(status);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    @Test
    void disableSetsStatusAndRecordsAudit() {
        UserAccount account = account(5L, "member@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(account));

        service.disable(5L, "admin@example.com");

        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.DISABLED);
        verify(userAccountRepository).save(account);
        verify(auditLogService).record(AuditLogService.ACTION_ACCOUNT_DISABLED, AuditLogService.ENTITY_USER_ACCOUNT,
                5L, "Disabled account member@example.com", "admin@example.com");
    }

    @Test
    void disableRejectsSelfDisable() {
        UserAccount account = account(5L, "admin@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.disable(5L, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void disableThrowsWhenAccountNotFound() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disable(99L, "admin@example.com"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void enableSetsStatusAndRecordsAudit() {
        UserAccount account = account(5L, "member@example.com", UserAccountStatus.DISABLED);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(account));

        service.enable(5L, "admin@example.com");

        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        verify(auditLogService).record(AuditLogService.ACTION_ACCOUNT_ENABLED, AuditLogService.ENTITY_USER_ACCOUNT,
                5L, "Re-enabled account member@example.com", "admin@example.com");
    }

    @Test
    void listAllIncludesLinkedPersonNameWhenVerifiedLinkExists() {
        UserAccount account = account(6L, "linked@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findAll()).thenReturn(List.of(account));

        Person person = new Person();
        UserPersonLink link = new UserPersonLink();
        link.setPerson(person);
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of(link));
        when(personDisplay.englishFullName(person)).thenReturn("Yuva Raj Bhatta");

        List<AdminUserAccountDto> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).linkedPersonName()).isEqualTo("Yuva Raj Bhatta");
    }

    @Test
    void listAllLeavesLinkedPersonNameNullWhenNoVerifiedLink() {
        UserAccount account = account(7L, "unlinked@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findAll()).thenReturn(List.of(account));
        when(userPersonLinkRepository.findByUserAccountId(7L)).thenReturn(List.of());

        List<AdminUserAccountDto> result = service.listAll();

        assertThat(result.get(0).linkedPersonName()).isNull();
    }

    @Test
    void listAllIncludesSubmittedInfoFromMostRecentVerificationRequest() {
        UserAccount account = account(8L, "applicant@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findAll()).thenReturn(List.of(account));
        when(userPersonLinkRepository.findByUserAccountId(8L)).thenReturn(List.of());

        VerificationRequest older = new VerificationRequest();
        older.setSubmittedFullName("Old Name");
        older.setCreatedAt(LocalDateTime.now().minusDays(2));
        VerificationRequest newer = new VerificationRequest();
        newer.setSubmittedFullName("Yuva Raj Bhatta");
        newer.setSubmittedFatherName("Bhoj Raj Bhatta");
        newer.setSubmittedGrandfatherName("Jhanka Nath Bhatta");
        newer.setCreatedAt(LocalDateTime.now());
        when(verificationRequestRepository.findByUserAccountId(8L)).thenReturn(List.of(older, newer));

        List<AdminUserAccountDto> result = service.listAll();

        assertThat(result.get(0).submittedFullName()).isEqualTo("Yuva Raj Bhatta");
    }

    @Test
    void linkCreatesAVerifiedLinkAndLogsIt() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        Person person = new Person();
        person.setId(416L);
        when(personRepository.findById(416L)).thenReturn(Optional.of(person));
        when(personDisplay.englishFullName(person)).thenReturn("Yuva Raj Bhatta");

        service.link(6L, 416L, "admin");

        verify(userPersonLinkService).createVerifiedLink(account, person);

        verify(auditLogService).record(
                org.mockito.ArgumentMatchers.eq(AuditLogService.ACTION_ACCOUNT_LINKED),
                org.mockito.ArgumentMatchers.eq(AuditLogService.ENTITY_USER_ACCOUNT),
                org.mockito.ArgumentMatchers.eq(6L),
                org.mockito.ArgumentMatchers.contains("Yuva Raj Bhatta"),
                org.mockito.ArgumentMatchers.eq("admin"));
    }

    @Test
    void linkThrowsWhenPersonMissing() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(6L, 999L, "admin")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void linkPropagatesIllegalArgumentExceptionWhenAccountAlreadyHasAVerifiedLink() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        Person person = new Person();
        person.setId(416L);
        when(personRepository.findById(416L)).thenReturn(Optional.of(person));

        when(userPersonLinkService.createVerifiedLink(account, person))
                .thenThrow(new IllegalArgumentException("This account is already linked to a person. Unlink it first."));

        assertThatThrownBy(() -> service.link(6L, 416L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked to a person");
    }

    @Test
    void linkPropagatesIllegalArgumentExceptionWhenPersonAlreadyVerifiedLinkedToAnotherAccount() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        Person person = new Person();
        person.setId(416L);
        when(personRepository.findById(416L)).thenReturn(Optional.of(person));

        when(userPersonLinkService.createVerifiedLink(account, person))
                .thenThrow(new IllegalArgumentException("Bhojraj Bhatta is already linked to another account."));

        assertThatThrownBy(() -> service.link(6L, 416L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bhojraj Bhatta")
                .hasMessageContaining("already linked to another account");
    }

    @Test
    void unlinkRemovesTheVerifiedLinkAndLogsIt() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        UserPersonLink verifiedLink = new UserPersonLink();
        verifiedLink.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of(verifiedLink));

        service.unlink(6L, "admin");

        verify(userPersonLinkRepository).deleteAll(List.of(verifiedLink));
        verify(auditLogService).record(AuditLogService.ACTION_ACCOUNT_UNLINKED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "Unlinked account yuva@example.com", "admin");
    }

    @Test
    void unlinkThrowsWhenNoVerifiedLinkExists() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.unlink(6L, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateSignupInfoEditsTheMostRecentVerificationRequest() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        VerificationRequest mostRecent = new VerificationRequest();
        mostRecent.setSubmittedFullName("Fake Name");
        mostRecent.setCreatedAt(LocalDateTime.now());
        when(verificationRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(mostRecent));

        AdminAccountSignupInfoUpdateDto request = new AdminAccountSignupInfoUpdateDto();
        request.setFullName("Yuva Raj Bhatta");
        request.setFatherName("Bhoj Raj Bhatta");
        request.setMotherName("Sita Bhatta");
        request.setGrandfatherName("Jhanka Nath Bhatta");
        request.setDobAd(LocalDate.of(1995, 6, 15));

        service.updateSignupInfo(6L, request, "admin");

        assertThat(mostRecent.getSubmittedFullName()).isEqualTo("Yuva Raj Bhatta");
        assertThat(mostRecent.getSubmittedFatherName()).isEqualTo("Bhoj Raj Bhatta");
        assertThat(mostRecent.getMotherName()).isEqualTo("Sita Bhatta");
        assertThat(mostRecent.getSubmittedGrandfatherName()).isEqualTo("Jhanka Nath Bhatta");
        assertThat(mostRecent.getSubmittedDobAd()).isEqualTo(LocalDate.of(1995, 6, 15));
        verify(verificationRequestRepository).save(mostRecent);
        verify(auditLogService).record(AuditLogService.ACTION_ACCOUNT_SIGNUP_INFO_EDITED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "Edited submitted signup info for yuva@example.com", "admin");
    }

    @Test
    void updateSignupInfoThrowsWhenNoSignupRecordExists() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(verificationRequestRepository.findByUserAccountId(6L)).thenReturn(List.of());

        AdminAccountSignupInfoUpdateDto request = new AdminAccountSignupInfoUpdateDto();

        assertThatThrownBy(() -> service.updateSignupInfo(6L, request, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteWipesOwnedRowsAndNullsBackwardReferences() {
        UserAccount account = account(6L, "fake@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        VerificationRequest ownSignup = new VerificationRequest();
        when(verificationRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(ownSignup));

        VerificationRequest reviewedByThisAccount = new VerificationRequest();
        reviewedByThisAccount.setReviewedBy(account);
        when(verificationRequestRepository.findByReviewedById(6L)).thenReturn(List.of(reviewedByThisAccount));

        UserPersonLink ownLink = new UserPersonLink();
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of(ownLink));

        UserPersonLink verifiedByThisAccount = new UserPersonLink();
        verifiedByThisAccount.setVerifiedBy(account);
        when(userPersonLinkRepository.findByVerifiedById(6L)).thenReturn(List.of(verifiedByThisAccount));

        when(personCorrectionRequestRepository.findBySubmittedById(6L)).thenReturn(List.of());

        com.familytree.entity.AdminAccessRequest ownAccessRequest = new com.familytree.entity.AdminAccessRequest();
        when(adminAccessRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(ownAccessRequest));

        com.familytree.entity.UserAccountToken ownToken = new com.familytree.entity.UserAccountToken();
        when(userAccountTokenRepository.findByUserAccountId(6L)).thenReturn(List.of(ownToken));

        service.delete(6L, "admin");

        assertThat(reviewedByThisAccount.getReviewedBy()).isNull();
        assertThat(verifiedByThisAccount.getVerifiedBy()).isNull();
        verify(userPersonLinkRepository).deleteAll(List.of(ownLink));
        verify(verificationRequestRepository).deleteAll(List.of(ownSignup));
        verify(adminAccessRequestRepository).deleteAll(List.of(ownAccessRequest));
        verify(userAccountTokenRepository).deleteAll(List.of(ownToken));
        verify(userAccountRepository).delete(account);
        verify(auditLogService).record(AuditLogService.ACTION_ACCOUNT_DELETED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "Deleted account fake@example.com", "admin");
    }

    @Test
    void deleteRejectsSelfDelete() {
        UserAccount account = account(5L, "admin@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.delete(5L, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userAccountRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsWhenTargetAccountHasAdminAccess() {
        UserAccount account = account(6L, "otheradmin@example.com", UserAccountStatus.ACTIVE);
        account.setRoles(new java.util.HashSet<>(List.of(role("ADMINISTRATOR"))));
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.delete(6L, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userAccountRepository, never()).delete(any());
    }

    @Test
    void listAllReportsIsAdminWhenAccountHasAdministratorRole() {
        UserAccount account = account(9L, "admin2@example.com", UserAccountStatus.ACTIVE);
        account.setRoles(new java.util.HashSet<>(List.of(role("ADMINISTRATOR"))));
        when(userAccountRepository.findAll()).thenReturn(List.of(account));
        when(userPersonLinkRepository.findByUserAccountId(9L)).thenReturn(List.of());

        assertThat(service.listAll().get(0).isAdmin()).isTrue();
    }

    @Test
    void listAllReportsNotAdminWhenAccountHasNoAdminRole() {
        UserAccount account = account(10L, "member@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findAll()).thenReturn(List.of(account));
        when(userPersonLinkRepository.findByUserAccountId(10L)).thenReturn(List.of());

        assertThat(service.listAll().get(0).isAdmin()).isFalse();
    }

    @Test
    void applySignupInfoToPersonSplitsFullNameAndAppliesDob() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        Person person = new Person();
        person.setId(416L);
        UserPersonLink verifiedLink = new UserPersonLink();
        verifiedLink.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        verifiedLink.setPerson(person);
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of(verifiedLink));

        VerificationRequest mostRecent = new VerificationRequest();
        mostRecent.setSubmittedFullName("Yuva Raj Bhatta");
        mostRecent.setSubmittedDobAd(LocalDate.of(1995, 6, 15));
        mostRecent.setCreatedAt(LocalDateTime.now());
        when(verificationRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(mostRecent));
        when(personDisplay.englishFullName(person)).thenReturn("Yuva Raj Bhatta");

        service.applySignupInfoToPerson(6L, "admin");

        assertThat(person.getFirstName()).isEqualTo("Yuva");
        assertThat(person.getMiddleName()).isEqualTo("Raj");
        assertThat(person.getLastName()).isEqualTo("Bhatta");
        assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
        verify(personRepository).save(person);
        verify(auditLogService).record(
                org.mockito.ArgumentMatchers.eq(AuditLogService.ACTION_PERSON_UPDATED),
                org.mockito.ArgumentMatchers.eq(AuditLogService.ENTITY_PERSON),
                org.mockito.ArgumentMatchers.eq(416L),
                org.mockito.ArgumentMatchers.contains("yuva@example.com"),
                org.mockito.ArgumentMatchers.eq("admin"));
    }

    @Test
    void applySignupInfoToPersonThrowsWhenNotLinked() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.applySignupInfoToPerson(6L, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applySignupInfoToPersonThrowsWhenNoSignupRecord() {
        UserAccount account = account(6L, "yuva@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        Person person = new Person();
        UserPersonLink verifiedLink = new UserPersonLink();
        verifiedLink.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        verifiedLink.setPerson(person);
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of(verifiedLink));
        when(verificationRequestRepository.findByUserAccountId(6L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.applySignupInfoToPerson(6L, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeAdminAccessRemovesAdminRolesAndLogsIt() {
        UserAccount account = account(6L, "member@example.com", UserAccountStatus.ACTIVE);
        account.setRoles(new java.util.HashSet<>(List.of(role("ADMINISTRATOR"), role("VERIFIED_MEMBER"))));
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        service.revokeAdminAccess(6L, "admin");

        assertThat(account.getRoles()).extracting(Role::getName).containsExactly("VERIFIED_MEMBER");
        verify(userAccountRepository).save(account);
        verify(auditLogService).record(AuditLogService.ACTION_ADMIN_ACCESS_REVOKED, AuditLogService.ENTITY_USER_ACCOUNT,
                6L, "Revoked admin access for member@example.com", "admin");
    }

    @Test
    void revokeAdminAccessThrowsWhenAccountHasNoAdminRole() {
        UserAccount account = account(6L, "member@example.com", UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.revokeAdminAccess(6L, "admin"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void revokeAdminAccessRejectsSelfRevoke() {
        UserAccount account = account(5L, "admin@example.com", UserAccountStatus.ACTIVE);
        account.setRoles(new java.util.HashSet<>(List.of(role("ADMINISTRATOR"))));
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.revokeAdminAccess(5L, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userAccountRepository, never()).save(any());
    }
}
