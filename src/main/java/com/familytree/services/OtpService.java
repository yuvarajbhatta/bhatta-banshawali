package com.familytree.services;

import com.familytree.entity.OtpPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountOtp;
import com.familytree.repository.UserAccountOtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Generates, hashes, and verifies single-use expiring 6-digit codes for
 * email verification and admin-access-request confirmation (see
 * OtpPurpose) -- the OTP counterpart to TokenService's link tokens. Only
 * the SHA-256 hash of a code is persisted, and each code allows a capped
 * number of guesses before it's locked out, since a 6-digit space is
 * small enough to brute-force with unlimited attempts.
 */
@Service
public class OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_BOUND = 1_000_000;

    private final UserAccountOtpRepository userAccountOtpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(UserAccountOtpRepository userAccountOtpRepository) {
        this.userAccountOtpRepository = userAccountOtpRepository;
    }

    /**
     * Invalidates any existing unconsumed OTP of this purpose for this
     * account first -- at most one live code per purpose per account, so
     * an old unused code silently stops working once a new one is issued.
     * Returns the RAW code (never persisted) for the caller to email.
     */
    @Transactional
    public String generate(UserAccount account, OtpPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();

        List<UserAccountOtp> existingLiveOtps =
                userAccountOtpRepository.findByUserAccountIdAndPurposeAndConsumedAtIsNull(account.getId(), purpose);
        for (UserAccountOtp existing : existingLiveOtps) {
            existing.setConsumedAt(now);
        }
        userAccountOtpRepository.saveAll(existingLiveOtps);

        String rawCode = String.format("%06d", secureRandom.nextInt(CODE_BOUND));

        UserAccountOtp otp = new UserAccountOtp();
        otp.setUserAccount(account);
        otp.setPurpose(purpose);
        otp.setCodeHash(hash(rawCode));
        otp.setAttemptCount(0);
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plus(OTP_TTL));
        userAccountOtpRepository.save(otp);

        return rawCode;
    }

    /**
     * Checks a raw code against the account's latest unconsumed OTP of
     * this purpose. A wrong guess counts against the attempt limit; once
     * exhausted, the code is locked out even if the correct code is later
     * supplied (the caller must request a fresh one). Correct guesses
     * consume the OTP so it can't be reused.
     */
    @Transactional
    public OtpVerifyResult verify(UserAccount account, OtpPurpose purpose, String rawCode) {
        Optional<UserAccountOtp> found = userAccountOtpRepository
                .findFirstByUserAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(account.getId(), purpose);
        if (found.isEmpty()) {
            return OtpVerifyResult.NOT_FOUND;
        }

        UserAccountOtp otp = found.get();
        if (otp.getConsumedAt() != null) {
            // Defense-in-depth: the query above already filters on
            // consumedAtIsNull, but matching TokenService's belt-and-suspenders
            // check keeps this safe even if that ever changes.
            return OtpVerifyResult.NOT_FOUND;
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return OtpVerifyResult.EXPIRED;
        }
        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            return OtpVerifyResult.TOO_MANY_ATTEMPTS;
        }

        if (!hash(rawCode).equals(otp.getCodeHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            userAccountOtpRepository.save(otp);
            return OtpVerifyResult.INVALID_CODE;
        }

        otp.setConsumedAt(LocalDateTime.now());
        userAccountOtpRepository.save(otp);
        return OtpVerifyResult.OK;
    }

    private String hash(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must always be available", e);
        }
    }
}
