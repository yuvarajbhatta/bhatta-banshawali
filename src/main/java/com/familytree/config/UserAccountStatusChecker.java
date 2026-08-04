package com.familytree.config;

import com.familytree.entity.UserAccountStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * Installed as DaoAuthenticationProvider's postAuthenticationChecks --
 * which runs AFTER the password has already been verified correct (see
 * AbstractUserDetailsAuthenticationProvider.authenticate()'s ordering:
 * preAuthenticationChecks, then password comparison, then
 * postAuthenticationChecks). A plain UserDetails (AppUser-backed legacy
 * admin logins) has no status concept and always passes; a
 * UserAccountPrincipal whose status isn't ACTIVE fails here, after the
 * password check already happened, not before.
 */
public class UserAccountStatusChecker implements UserDetailsChecker {

    @Override
    public void check(UserDetails userDetails) {
        if (userDetails instanceof UserAccountPrincipal principal && principal.getStatus() != UserAccountStatus.ACTIVE) {
            throw new AccountNotActiveException(principal.getStatus());
        }
    }
}
