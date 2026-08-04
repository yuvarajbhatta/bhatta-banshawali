package com.familytree.config;

import com.familytree.entity.AppUser;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates against either the old AppUser table (username/password,
 * ROLE_ADMIN or ROLE_USER) or the new UserAccount table (email/password,
 * Role entities) -- introduced alongside AppUser per the Phase 1 plan,
 * not replacing it. AppUser is checked first since every account there
 * already works today; UserAccount is the fallback for anyone who's
 * signed up.
 *
 * A UserAccount authenticates by email in the same "username" field the
 * login form already has -- there's no second form or field, the login
 * page label just says "Username or Email" now.
 *
 * Deliberately does NOT filter UserAccount lookups by status anymore
 * (that used to happen here, throwing UsernameNotFoundException for
 * anything but ACTIVE) -- the status is carried on the returned
 * UserAccountPrincipal instead, and gated later by
 * UserAccountStatusChecker as DaoAuthenticationProvider's
 * postAuthenticationChecks, which only runs after the password has
 * already been verified. That's what lets a pending/disabled account
 * with the *correct* password get a specific, helpful login error while
 * a wrong email or wrong password for any account still gets the same
 * generic "invalid credentials" either way -- see
 * docs/05-auth-and-verification.md's anti-enumeration guarantees.
 */
public class BridgingUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final UserAccountRepository userAccountRepository;

    public BridgingUserDetailsService(AppUserRepository appUserRepository, UserAccountRepository userAccountRepository) {
        this.appUserRepository = appUserRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return appUserRepository.findByUsername(usernameOrEmail)
                .map(this::toUserDetails)
                .or(() -> findUserAccount(usernameOrEmail).map(this::toUserDetails))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }

    private Optional<UserAccount> findUserAccount(String usernameOrEmail) {
        String normalizedEmail = usernameOrEmail == null ? "" : usernameOrEmail.trim().toLowerCase();
        return userAccountRepository.findByEmailWithRoles(normalizedEmail);
    }

    // The legacy AppUser table predates the whole UserAccount/Role/verification
    // workflow and is the site owner's own account (confirmed: production has
    // exactly one ROLE_ADMIN row here) -- it's granted SUPER_ADMIN too so the
    // owner is never the one account that could get locked out of
    // super-admin-gated actions (see SecurityConfig's admin-access-request
    // approval rule, docs/09-security-threat-model.md item 13).
    private UserDetails toUserDetails(AppUser user) {
        String role = user.getRole().replace("ROLE_", "");
        String[] roles = "ADMIN".equals(role) ? new String[] {"ADMIN", "SUPER_ADMIN"} : new String[] {role};
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(roles)
                .build();
    }

    private UserDetails toUserDetails(UserAccount account) {
        boolean isSuperAdmin = account.getRoles().stream().anyMatch(role -> "SUPER_ADMINISTRATOR".equals(role.getName()));
        boolean isAdmin = isSuperAdmin || account.getRoles().stream().anyMatch(role -> "ADMINISTRATOR".equals(role.getName()));

        List<String> roles = new ArrayList<>();
        if (isAdmin) {
            roles.add("ADMIN");
        }
        if (isSuperAdmin) {
            roles.add("SUPER_ADMIN");
        }
        if (roles.isEmpty()) {
            roles.add("USER");
        }

        return new UserAccountPrincipal(account.getEmail(), account.getPasswordHash(), account.getStatus(), roles);
    }
}
