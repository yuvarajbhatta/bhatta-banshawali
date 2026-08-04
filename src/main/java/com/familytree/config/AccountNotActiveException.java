package com.familytree.config;

import com.familytree.entity.UserAccountStatus;
import org.springframework.security.core.AuthenticationException;

/**
 * Thrown by UserAccountStatusChecker when a login's password was correct
 * but the account isn't ACTIVE yet -- see UserAccountPrincipal for why
 * that ordering (only after password verification) is what makes this
 * safe to surface. LoginFailureHandler turns this into a specific
 * ?error=... reason on the redirect back to the login page instead of
 * the generic one.
 */
public class AccountNotActiveException extends AuthenticationException {

    private final UserAccountStatus status;

    public AccountNotActiveException(UserAccountStatus status) {
        super("Account is not active: " + status);
        this.status = status;
    }

    public UserAccountStatus getStatus() {
        return status;
    }
}
