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

}
