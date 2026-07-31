package com.familytree.services;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestResetIssuesATokenAndSendsTheEmailForAnExistingAccount() {
        UserAccount account = new UserAccount();
        account.setEmail("yuva@example.com");
        account.setPreferredLanguage("en");
        when(userAccountRepository.findByEmail("yuva@example.com")).thenReturn(Optional.of(account));
        when(tokenService.issueToken(account, TokenPurpose.PASSWORD_RESET)).thenReturn("raw-token");

        passwordResetService.requestReset("yuva@example.com");

        verify(emailService).sendPasswordResetEmail("yuva@example.com", "raw-token", "en");
    }

    @Test
    void requestResetThrowsAccountNotFoundForAnUnknownEmailAndTouchesNothingElse() {
        when(userAccountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.requestReset("nobody@example.com"))
                .isInstanceOf(AccountNotFoundException.class);

        verify(tokenService, never()).issueToken(any(), any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void requestResetNormalizesEmail() {
        UserAccount account = new UserAccount();
        account.setEmail("yuva@example.com");
        when(userAccountRepository.findByEmail("yuva@example.com")).thenReturn(Optional.of(account));
        when(tokenService.issueToken(any(), eq(TokenPurpose.PASSWORD_RESET))).thenReturn("raw-token");

        passwordResetService.requestReset("  Yuva@Example.com  ");

        verify(userAccountRepository).findByEmail("yuva@example.com");
    }

    @Test
    void requestResetSwallowsAnEmailSendFailure() {
        UserAccount account = new UserAccount();
        account.setEmail("yuva@example.com");
        when(userAccountRepository.findByEmail("yuva@example.com")).thenReturn(Optional.of(account));
        when(tokenService.issueToken(any(), eq(TokenPurpose.PASSWORD_RESET))).thenReturn("raw-token");
        doThrow(new RuntimeException("SMTP down")).when(emailService).sendPasswordResetEmail(any(), any(), any());

        passwordResetService.requestReset("yuva@example.com");
        // No exception propagates -- reaching this line is the assertion.
    }

    @Test
    void confirmResetUpdatesThePasswordHashAndConsumesTheToken() {
        UserAccount account = new UserAccount();
        when(tokenService.consumeToken("raw-token", TokenPurpose.PASSWORD_RESET)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("newPassword123")).thenReturn("{bcrypt}newHash");

        passwordResetService.confirmReset("raw-token", "newPassword123");

        assertThat(account.getPasswordHash()).isEqualTo("{bcrypt}newHash");
        verify(userAccountRepository).save(account);
    }

    @Test
    void confirmResetThrowsForAnInvalidOrExpiredToken() {
        when(tokenService.consumeToken("garbage", TokenPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.confirmReset("garbage", "newPassword123"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(userAccountRepository, never()).save(any());
    }
}
