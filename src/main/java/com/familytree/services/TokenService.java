package com.familytree.services;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountToken;
import com.familytree.repository.UserAccountTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Generates, hashes, and consumes single-use expiring tokens for password
 * reset (see TokenPurpose). The raw token is only ever held in memory long
 * enough to put in an email link -- only its SHA-256 hash is persisted, so
 * a stolen database dump can't be used to reset anyone's password
 * directly. Email verification used to share this table/purpose but now
 * uses OtpService/UserAccountOtp instead, since a code the user types is a
 * better fit than a link for that flow.
 */
@Service
public class TokenService {

    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final int RAW_TOKEN_BYTES = 32;

    private final UserAccountTokenRepository userAccountTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(UserAccountTokenRepository userAccountTokenRepository) {
        this.userAccountTokenRepository = userAccountTokenRepository;
    }

    /**
     * Invalidates any existing unconsumed token of this purpose for this
     * account first -- at most one live token per purpose per account, so
     * an old unused link silently stops working once a new one is issued.
     * Returns the RAW token (never persisted) for the caller to put in an
     * email link.
     */
    @Transactional
    public String issueToken(UserAccount account, TokenPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();

        List<UserAccountToken> existingLiveTokens =
                userAccountTokenRepository.findByUserAccountIdAndPurposeAndConsumedAtIsNull(account.getId(), purpose);
        for (UserAccountToken existing : existingLiveTokens) {
            existing.setConsumedAt(now);
        }
        userAccountTokenRepository.saveAll(existingLiveTokens);

        byte[] rawBytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(rawBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

        UserAccountToken token = new UserAccountToken();
        token.setUserAccount(account);
        token.setPurpose(purpose);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(PASSWORD_RESET_TTL));
        userAccountTokenRepository.save(token);

        return rawToken;
    }

    /**
     * Validates and consumes a raw token in one transactional step -- no
     * window between "check valid" and "mark consumed" for a second
     * concurrent request to race through. Returns empty if the token
     * doesn't exist, was already consumed, or has expired.
     */
    @Transactional
    public Optional<UserAccount> consumeToken(String rawToken, TokenPurpose purpose) {
        Optional<UserAccountToken> found = userAccountTokenRepository.findByTokenHashAndPurpose(hash(rawToken), purpose);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        UserAccountToken token = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (token.getConsumedAt() != null || token.getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }

        token.setConsumedAt(now);
        userAccountTokenRepository.save(token);
        return Optional.of(token.getUserAccount());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must always be available", e);
        }
    }
}
