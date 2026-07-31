package com.familytree.services;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Forgot password" for UserAccount logins (the legacy AppUser admin
 * account has no email field at all, so it's out of scope here entirely
 * -- see docs/05-auth-and-verification.md). Allows requesting/consuming a
 * reset regardless of UserAccountStatus: resetting a non-ACTIVE account's
 * password reveals nothing new since login is still gated on ACTIVE
 * elsewhere (BridgingUserDetailsService), unchanged by this class.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserAccountRepository userAccountRepository,
                                 TokenService tokenService,
                                 EmailService emailService,
                                 PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @throws AccountNotFoundException if no account matches the email --
     *          see that class's javadoc for why this isn't a silent no-op.
     */
    @Transactional
    public void requestReset(String email) {
        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new AccountNotFoundException("No account found with that email."));

        String rawToken = tokenService.issueToken(account, TokenPurpose.PASSWORD_RESET);
        try {
            emailService.sendPasswordResetEmail(account.getEmail(), rawToken, account.getPreferredLanguage());
        } catch (Exception e) {
            // Best-effort: a transient SMTP failure must not surface as a
            // 500 to the caller -- the token still exists, so a retried
            // request (or the user asking support directly) still works.
            log.warn("Failed to send password reset email to {}", account.getEmail(), e);
        }
    }

    /**
     * @throws InvalidOrExpiredTokenException if the token doesn't exist,
     *          was already consumed, or has expired.
     */
    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        UserAccount account = tokenService.consumeToken(rawToken, TokenPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("This reset link is invalid or has expired."));
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(account);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
