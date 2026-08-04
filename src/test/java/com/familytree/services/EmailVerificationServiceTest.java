package com.familytree.services;

import com.familytree.entity.OtpPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void confirmVerificationSetsEmailVerifiedAt() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));
        when(otpService.verify(account, OtpPurpose.EMAIL_VERIFICATION, "123456")).thenReturn(OtpVerifyResult.OK);

        emailVerificationService.confirmVerification("member@example.com", "123456");

        assertThat(account.getEmailVerifiedAt()).isNotNull();
        verify(userAccountRepository).save(account);
    }

    @Test
    void confirmVerificationThrowsForAnInvalidCode() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));
        when(otpService.verify(account, OtpPurpose.EMAIL_VERIFICATION, "000000")).thenReturn(OtpVerifyResult.INVALID_CODE);

        assertThatThrownBy(() -> emailVerificationService.confirmVerification("member@example.com", "000000"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void confirmVerificationThrowsForAnUnknownEmail() {
        when(userAccountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.confirmVerification("nobody@example.com", "123456"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(otpService, never()).verify(any(), any(), any());
    }

    @Test
    void resendVerificationIssuesAFreshOtpForAnUnverifiedAccount() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        account.setPreferredLanguage("en");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));
        when(otpService.generate(account, OtpPurpose.EMAIL_VERIFICATION)).thenReturn("654321");

        emailVerificationService.resendVerification("member@example.com");

        verify(emailService).sendVerificationOtpEmail("member@example.com", "654321", "en");
    }

    @Test
    void resendVerificationNoOpsWhenTheEmailIsAlreadyVerified() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        account.setEmailVerifiedAt(java.time.LocalDateTime.now());
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));

        emailVerificationService.resendVerification("member@example.com");

        verify(otpService, never()).generate(any(), any());
        verify(emailService, never()).sendVerificationOtpEmail(any(), any(), any());
    }

    @Test
    void resendVerificationNoOpsForAnUnknownEmail() {
        when(userAccountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        emailVerificationService.resendVerification("nobody@example.com");

        verify(otpService, never()).generate(any(), any());
    }
}
