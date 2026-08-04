package com.familytree.services;

import com.familytree.entity.OtpPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountOtp;
import com.familytree.repository.UserAccountOtpRepository;
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
class OtpServiceTest {

    @Mock
    private UserAccountOtpRepository userAccountOtpRepository;

    @InjectMocks
    private OtpService otpService;

    @Test
    void generatePersistsTheSha256HashOfTheReturnedSixDigitCode() {
        UserAccount account = new UserAccount();
        when(userAccountOtpRepository.findByUserAccountIdAndPurposeAndConsumedAtIsNull(any(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                .thenReturn(List.of());

        String rawCode = otpService.generate(account, OtpPurpose.EMAIL_VERIFICATION);

        assertThat(rawCode).hasSize(6).containsOnlyDigits();

        ArgumentCaptor<UserAccountOtp> captor = ArgumentCaptor.forClass(UserAccountOtp.class);
        verify(userAccountOtpRepository).save(captor.capture());
        assertThat(captor.getValue().getCodeHash()).isEqualTo(sha256Hex(rawCode));
        assertThat(captor.getValue().getPurpose()).isEqualTo(OtpPurpose.EMAIL_VERIFICATION);
        assertThat(captor.getValue().getAttemptCount()).isZero();
        assertThat(captor.getValue().getConsumedAt()).isNull();
    }

    @Test
    void generatingASecondCodeInvalidatesTheFirst() {
        UserAccount account = new UserAccount();
        UserAccountOtp existingLiveOtp = new UserAccountOtp();
        existingLiveOtp.setUserAccount(account);
        existingLiveOtp.setPurpose(OtpPurpose.ADMIN_ACCESS_REQUEST);
        when(userAccountOtpRepository.findByUserAccountIdAndPurposeAndConsumedAtIsNull(any(), eq(OtpPurpose.ADMIN_ACCESS_REQUEST)))
                .thenReturn(List.of(existingLiveOtp));

        otpService.generate(account, OtpPurpose.ADMIN_ACCESS_REQUEST);

        assertThat(existingLiveOtp.getConsumedAt()).isNotNull();
        verify(userAccountOtpRepository).saveAll(List.of(existingLiveOtp));
    }

    @Test
    void verifySucceedsOnceThenFailsOnASecondAttempt() {
        UserAccount account = new UserAccount();
        UserAccountOtp otp = new UserAccountOtp();
        otp.setUserAccount(account);
        otp.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        otp.setCodeHash(sha256Hex("123456"));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        // Same instance returned both times: verify mutates it in-place
        // (sets consumedAt on success), so the second lookup naturally
        // sees it as already consumed -- exactly like a real repository.
        when(userAccountOtpRepository.findFirstByUserAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn(Optional.of(otp));

        OtpVerifyResult first = otpService.verify(account, OtpPurpose.EMAIL_VERIFICATION, "123456");
        OtpVerifyResult second = otpService.verify(account, OtpPurpose.EMAIL_VERIFICATION, "123456");

        assertThat(first).isEqualTo(OtpVerifyResult.OK);
        assertThat(second).isEqualTo(OtpVerifyResult.NOT_FOUND);
    }

    @Test
    void verifyFailsWhenExpired() {
        UserAccountOtp otp = new UserAccountOtp();
        otp.setUserAccount(new UserAccount());
        otp.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        otp.setCodeHash(sha256Hex("123456"));
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userAccountOtpRepository.findFirstByUserAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn(Optional.of(otp));

        OtpVerifyResult result = otpService.verify(new UserAccount(), OtpPurpose.EMAIL_VERIFICATION, "123456");

        assertThat(result).isEqualTo(OtpVerifyResult.EXPIRED);
    }

    @Test
    void verifyFailsWhenNoOtpIsPending() {
        when(userAccountOtpRepository.findFirstByUserAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn(Optional.empty());

        OtpVerifyResult result = otpService.verify(new UserAccount(), OtpPurpose.EMAIL_VERIFICATION, "123456");

        assertThat(result).isEqualTo(OtpVerifyResult.NOT_FOUND);
    }

    @Test
    void wrongGuessesIncrementAttemptCountAndEventuallyLockOutTheOtp() {
        UserAccountOtp otp = new UserAccountOtp();
        otp.setUserAccount(new UserAccount());
        otp.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        otp.setCodeHash(sha256Hex("123456"));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otp.setAttemptCount(0);
        when(userAccountOtpRepository.findFirstByUserAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn(Optional.of(otp));

        for (int i = 0; i < 5; i++) {
            OtpVerifyResult result = otpService.verify(new UserAccount(), OtpPurpose.EMAIL_VERIFICATION, "000000");
            assertThat(result).isEqualTo(OtpVerifyResult.INVALID_CODE);
        }
        assertThat(otp.getAttemptCount()).isEqualTo(5);

        // Attempts exhausted -- even the correct code is now rejected.
        OtpVerifyResult finalAttempt = otpService.verify(new UserAccount(), OtpPurpose.EMAIL_VERIFICATION, "123456");
        assertThat(finalAttempt).isEqualTo(OtpVerifyResult.TOO_MANY_ATTEMPTS);
    }

    private String sha256Hex(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawCode.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
