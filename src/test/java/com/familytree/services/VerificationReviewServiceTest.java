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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationReviewServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PersonDisplayHelper personDisplay;

    @InjectMocks
    private VerificationReviewService verificationReviewService;

    @Test
    void approveMarksRequestApprovedAndActivatesAccount() {
        UserAccount account = new UserAccount();
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(1L, "admin", "looks good", null);

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(request.getReviewedByUsername()).isEqualTo("admin");
        assertThat(request.getDecisionNote()).isEqualTo("looks good");
        assertThat(request.getReviewedAt()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);

        verify(verificationRequestRepository).save(request);
        verify(userAccountRepository).save(account);
    }

    @Test
    void approveGrantsVerifiedMemberRoleSoTheAccountCanActuallyLogInWithAccess() {
        Role verifiedMember = new Role();
        verifiedMember.setName("VERIFIED_MEMBER");
        when(roleRepository.findByName("VERIFIED_MEMBER")).thenReturn(Optional.of(verifiedMember));

        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(10L, "admin", null, null);

        assertThat(account.getRoles()).contains(verifiedMember);
    }

    @Test
    void approveDoesNotFailWhenVerifiedMemberRoleIsMissing() {
        when(roleRepository.findByName("VERIFIED_MEMBER")).thenReturn(Optional.empty());
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(11L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(11L, "admin", null, null);

        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(account.getRoles()).isEmpty();
    }

    @Test
    void approveWithLinkedPersonIdCreatesAVerifiedUserPersonLink() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(20L)).thenReturn(Optional.of(request));

        Person person = new Person();
        person.setId(77L);
        when(personRepository.findById(77L)).thenReturn(Optional.of(person));

        verificationReviewService.approve(20L, "admin", null, 77L);

        ArgumentCaptor<UserPersonLink> captor = ArgumentCaptor.forClass(UserPersonLink.class);
        verify(userPersonLinkRepository).save(captor.capture());
        UserPersonLink savedLink = captor.getValue();
        assertThat(savedLink.getUserAccount()).isEqualTo(account);
        assertThat(savedLink.getPerson()).isEqualTo(person);
        assertThat(savedLink.getLinkStatus()).isEqualTo(UserPersonLinkStatus.VERIFIED);
        assertThat(savedLink.getVerifiedAt()).isNotNull();
    }

    @Test
    void approveWithoutLinkedPersonIdNeverTouchesUserPersonLinkRepository() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(21L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(21L, "admin", null, null);

        org.mockito.Mockito.verifyNoInteractions(userPersonLinkRepository);
    }

    @Test
    void approveThrowsWhenLinkedPersonIdDoesNotExist() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> verificationReviewService.approve(22L, "admin", null, 999L));
    }

    @Test
    void rejectMarksRequestRejectedAndDisablesAccount() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(2L)).thenReturn(Optional.of(request));

        verificationReviewService.reject(2L, "admin", "no lineage match");

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.DISABLED);
    }

    @Test
    void requestMoreInfoLeavesAccountStatusUnchanged() {
        UserAccount account = new UserAccount();
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(3L)).thenReturn(Optional.of(request));

        verificationReviewService.requestMoreInfo(3L, "admin", "please clarify father's name");

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.NEEDS_MORE_INFO);
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        verify(userAccountRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void throwsWhenVerificationRequestNotFound() {
        when(verificationRequestRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> verificationReviewService.approve(99L, "admin", null, null));
    }

    @Test
    void capturesReviewerAndTimestampConsistentlyAcrossActions() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(4L)).thenReturn(Optional.of(request));

        verificationReviewService.reject(4L, "reviewer-x", null);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewedByUsername()).isEqualTo("reviewer-x");
        assertThat(captor.getValue().getReviewedAt()).isNotNull();
    }

    private UserAccount accountWithId(long id, UserAccountStatus status) {
        UserAccount account = new UserAccount();
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", id);
        account.setStatus(status);
        return account;
    }

    private UserPersonLink link(UserPersonLinkStatus status) {
        UserPersonLink link = new UserPersonLink();
        link.setLinkStatus(status);
        return link;
    }

    @Test
    void findUnlinkedActiveAccountsExcludesAccountsWithAVerifiedLink() {
        UserAccount linked = accountWithId(1L, UserAccountStatus.ACTIVE);
        UserAccount unlinked = accountWithId(2L, UserAccountStatus.ACTIVE);
        UserAccount pending = accountWithId(3L, UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        when(userAccountRepository.findAll()).thenReturn(List.of(linked, unlinked, pending));
        when(userPersonLinkRepository.findByUserAccountId(1L)).thenReturn(List.of(link(UserPersonLinkStatus.VERIFIED)));
        when(userPersonLinkRepository.findByUserAccountId(2L)).thenReturn(List.of());

        List<UserAccount> result = verificationReviewService.findUnlinkedActiveAccounts();

        assertThat(result).containsExactly(unlinked);
    }

    @Test
    void findUnlinkedActiveAccountsIncludesAccountsWithOnlyAPendingOrRejectedLink() {
        UserAccount account = accountWithId(5L, UserAccountStatus.ACTIVE);
        when(userAccountRepository.findAll()).thenReturn(List.of(account));
        when(userPersonLinkRepository.findByUserAccountId(5L))
                .thenReturn(List.of(link(UserPersonLinkStatus.PENDING), link(UserPersonLinkStatus.REJECTED)));

        assertThat(verificationReviewService.findUnlinkedActiveAccounts()).containsExactly(account);
    }

    @Test
    void linkAccountToPersonCreatesAVerifiedLinkAndLogsIt() {
        UserAccount account = accountWithId(6L, UserAccountStatus.ACTIVE);
        account.setEmail("yuva@example.com");
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));

        Person person = new Person();
        person.setId(416L);
        when(personRepository.findById(416L)).thenReturn(Optional.of(person));
        when(personDisplay.englishFullName(person)).thenReturn("Yuva Raj Bhatta");

        verificationReviewService.linkAccountToPerson(6L, 416L, "admin");

        ArgumentCaptor<UserPersonLink> captor = ArgumentCaptor.forClass(UserPersonLink.class);
        verify(userPersonLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAccount()).isEqualTo(account);
        assertThat(captor.getValue().getPerson()).isEqualTo(person);
        assertThat(captor.getValue().getLinkStatus()).isEqualTo(UserPersonLinkStatus.VERIFIED);

        verify(auditLogService).record(
                org.mockito.ArgumentMatchers.eq(AuditLogService.ACTION_ACCOUNT_LINKED),
                org.mockito.ArgumentMatchers.eq(AuditLogService.ENTITY_USER_ACCOUNT),
                org.mockito.ArgumentMatchers.eq(6L),
                org.mockito.ArgumentMatchers.contains("Yuva Raj Bhatta"),
                org.mockito.ArgumentMatchers.eq("admin"));
    }

    @Test
    void linkAccountToPersonThrowsWhenAccountMissing() {
        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> verificationReviewService.linkAccountToPerson(999L, 1L, "admin"));
    }

    @Test
    void linkAccountToPersonThrowsWhenPersonMissing() {
        UserAccount account = accountWithId(6L, UserAccountStatus.ACTIVE);
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(account));
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> verificationReviewService.linkAccountToPerson(6L, 999L, "admin"));
    }
}
