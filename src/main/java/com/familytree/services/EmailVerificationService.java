package com.familytree.services;

import com.familytree.entity.OtpPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.email.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Confirms a signup's email is real via a 6-digit OTP (see OtpService).
 * Deliberately decoupled from UserAccountStatus/admin approval (see
 * UserAccount.emailVerifiedAt's javadoc and SignupService) -- this is an
 * admin-review signal, not a login gate.
 */
@Service
public class EmailVerificationService {

    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    public EmailVerificationService(UserAccountRepository userAccountRepository, OtpService otpService,
                                    EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    /**
     * @throws InvalidOrExpiredTokenException if there's no account with
     *          this email, the code doesn't match, was already consumed,
     *          has expired, or has been guessed wrong too many times.
     */
    @Transactional
    public void confirmVerification(String email, String code) {
        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new InvalidOrExpiredTokenException("This verification code is invalid or has expired."));

        OtpVerifyResult result = otpService.verify(account, OtpPurpose.EMAIL_VERIFICATION, code);
        if (result != OtpVerifyResult.OK) {
            throw new InvalidOrExpiredTokenException(messageFor(result));
        }

        account.setEmailVerifiedAt(LocalDateTime.now());
        userAccountRepository.save(account);
    }

    /**
     * Re-issues a fresh OTP for a signed-up-but-not-yet-verified email --
     * lets the user recover from a lost or expired code without having to
     * sign up again. Silently no-ops for an unknown or already-verified
     * email so this endpoint can't be used to probe which emails exist.
     */
    @Transactional
    public void resendVerification(String email) {
        userAccountRepository.findByEmail(normalizeEmail(email))
                .filter(account -> account.getEmailVerifiedAt() == null)
                .ifPresent(account -> {
                    String code = otpService.generate(account, OtpPurpose.EMAIL_VERIFICATION);
                    emailService.sendVerificationOtpEmail(account.getEmail(), code, account.getPreferredLanguage());
                });
    }

    private String messageFor(OtpVerifyResult result) {
        return switch (result) {
            case EXPIRED -> "This code has expired. Request a new one.";
            case TOO_MANY_ATTEMPTS -> "Too many incorrect attempts. Request a new code.";
            case NOT_FOUND -> "No verification code is pending for this email. Request a new one.";
            default -> "This verification code is invalid.";
        };
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
