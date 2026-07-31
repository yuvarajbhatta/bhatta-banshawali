package com.familytree.services;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
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
    private TokenService tokenService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void confirmVerificationSetsEmailVerifiedAt() {
        UserAccount account = new UserAccount();
        when(tokenService.consumeToken("raw-token", TokenPurpose.EMAIL_VERIFICATION)).thenReturn(Optional.of(account));

        emailVerificationService.confirmVerification("raw-token");

        assertThat(account.getEmailVerifiedAt()).isNotNull();
        verify(userAccountRepository).save(account);
    }

    @Test
    void confirmVerificationThrowsForAnInvalidOrExpiredToken() {
        when(tokenService.consumeToken("garbage", TokenPurpose.EMAIL_VERIFICATION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.confirmVerification("garbage"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(userAccountRepository, never()).save(any());
    }
}
