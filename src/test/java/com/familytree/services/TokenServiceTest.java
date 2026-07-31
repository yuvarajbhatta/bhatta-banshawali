package com.familytree.services;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountToken;
import com.familytree.repository.UserAccountTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserAccountTokenRepository userAccountTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void issueTokenPersistsTheSha256HashOfTheReturnedRawToken() {
        UserAccount account = new UserAccount();
        when(userAccountTokenRepository.findByUserAccountIdAndPurposeAndConsumedAtIsNull(any(), eq(TokenPurpose.PASSWORD_RESET)))
                .thenReturn(List.of());

        String rawToken = tokenService.issueToken(account, TokenPurpose.PASSWORD_RESET);

        ArgumentCaptor<UserAccountToken> captor = ArgumentCaptor.forClass(UserAccountToken.class);
        verify(userAccountTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(sha256Hex(rawToken));
        assertThat(captor.getValue().getPurpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);
        assertThat(captor.getValue().getConsumedAt()).isNull();
    }

    @Test
    void issuingASecondTokenInvalidatesTheFirst() {
        UserAccount account = new UserAccount();
        UserAccountToken existingLiveToken = new UserAccountToken();
        existingLiveToken.setUserAccount(account);
        existingLiveToken.setPurpose(TokenPurpose.EMAIL_VERIFICATION);
        when(userAccountTokenRepository.findByUserAccountIdAndPurposeAndConsumedAtIsNull(any(), eq(TokenPurpose.EMAIL_VERIFICATION)))
                .thenReturn(List.of(existingLiveToken));

        tokenService.issueToken(account, TokenPurpose.EMAIL_VERIFICATION);

        assertThat(existingLiveToken.getConsumedAt()).isNotNull();
        verify(userAccountTokenRepository).saveAll(List.of(existingLiveToken));
    }

    @Test
    void consumeTokenSucceedsOnceThenFailsOnASecondAttempt() {
        UserAccountToken token = new UserAccountToken();
        token.setUserAccount(new UserAccount());
        token.setPurpose(TokenPurpose.PASSWORD_RESET);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        // Same instance returned both times: consumeToken mutates it
        // in-place (sets consumedAt), so the second lookup naturally sees
        // it as already consumed -- exactly like a real repository would.
        when(userAccountTokenRepository.findByTokenHashAndPurpose(any(), eq(TokenPurpose.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));

        Optional<UserAccount> firstAttempt = tokenService.consumeToken("raw-token", TokenPurpose.PASSWORD_RESET);
        Optional<UserAccount> secondAttempt = tokenService.consumeToken("raw-token", TokenPurpose.PASSWORD_RESET);

        assertThat(firstAttempt).isPresent();
        assertThat(secondAttempt).isEmpty();
    }

    @Test
    void consumeTokenFailsWhenExpired() {
        UserAccountToken token = new UserAccountToken();
        token.setUserAccount(new UserAccount());
        token.setPurpose(TokenPurpose.EMAIL_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userAccountTokenRepository.findByTokenHashAndPurpose(any(), eq(TokenPurpose.EMAIL_VERIFICATION)))
                .thenReturn(Optional.of(token));

        Optional<UserAccount> result = tokenService.consumeToken("raw-token", TokenPurpose.EMAIL_VERIFICATION);

        assertThat(result).isEmpty();
    }

    @Test
    void consumeTokenFailsWhenNoTokenMatchesTheHash() {
        when(userAccountTokenRepository.findByTokenHashAndPurpose(any(), eq(TokenPurpose.PASSWORD_RESET)))
                .thenReturn(Optional.empty());

        Optional<UserAccount> result = tokenService.consumeToken("garbage", TokenPurpose.PASSWORD_RESET);

        assertThat(result).isEmpty();
    }

    private String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
