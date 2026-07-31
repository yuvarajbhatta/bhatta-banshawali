package com.familytree.services;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Confirms a signup's email is real. Deliberately decoupled from
 * UserAccountStatus/admin approval (see UserAccount.emailVerifiedAt's
 * javadoc and SignupService) -- this is an admin-review signal, not a
 * login gate.
 */
@Service
public class EmailVerificationService {

    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;

    public EmailVerificationService(UserAccountRepository userAccountRepository, TokenService tokenService) {
        this.userAccountRepository = userAccountRepository;
        this.tokenService = tokenService;
    }

    /**
     * @throws InvalidOrExpiredTokenException if the token doesn't exist,
     *          was already consumed, or has expired.
     */
    @Transactional
    public void confirmVerification(String rawToken) {
        UserAccount account = tokenService.consumeToken(rawToken, TokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("This verification link is invalid or has expired."));
        account.setEmailVerifiedAt(LocalDateTime.now());
        userAccountRepository.save(account);
    }
}
