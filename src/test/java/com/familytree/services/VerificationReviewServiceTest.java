package com.familytree.services;

import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private VerificationReviewService verificationReviewService;

    @Test
    void approveMarksRequestApprovedAndActivatesAccount() {
        UserAccount account = new UserAccount();
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(1L, "admin", "looks good");

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(request.getReviewedByUsername()).isEqualTo("admin");
        assertThat(request.getDecisionNote()).isEqualTo("looks good");
        assertThat(request.getReviewedAt()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);

        verify(verificationRequestRepository).save(request);
        verify(userAccountRepository).save(account);
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
                () -> verificationReviewService.approve(99L, "admin", null));
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
