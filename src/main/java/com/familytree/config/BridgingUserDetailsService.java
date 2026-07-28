package com.familytree.config;

import com.familytree.entity.AppUser;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

/**
 * Authenticates against either the old AppUser table (username/password,
 * ROLE_ADMIN or ROLE_USER) or the new UserAccount table (email/password,
 * Role entities) -- introduced alongside AppUser per the Phase 1 plan,
 * not replacing it. AppUser is checked first since every account there
 * already works today; UserAccount is the fallback for anyone whose
 * signup has been approved.
 *
 * A UserAccount authenticates by email in the same "username" field the
 * login form already has -- there's no second form or field, the login
 * page label just says "Username or Email" now.
 *
 * Deliberately does not distinguish "no such account" from "account
 * exists but isn't ACTIVE yet" in the exception thrown -- both produce
 * the same generic UsernameNotFoundException, so a pending or rejected
 * applicant's login attempt looks identical to a wrong email, matching
 * the anti-enumeration requirement in docs/05-auth-and-verification.md.
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
                .or(() -> findActiveUserAccount(usernameOrEmail).map(this::toUserDetails))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }

    private Optional<UserAccount> findActiveUserAccount(String usernameOrEmail) {
        String normalizedEmail = usernameOrEmail == null ? "" : usernameOrEmail.trim().toLowerCase();
        return userAccountRepository.findByEmailWithRoles(normalizedEmail)
                .filter(account -> account.getStatus() == UserAccountStatus.ACTIVE);
    }

    private UserDetails toUserDetails(AppUser user) {
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", ""))
                .build();
    }

    private UserDetails toUserDetails(UserAccount account) {
        boolean isAdmin = account.getRoles().stream()
                .anyMatch(role -> "ADMINISTRATOR".equals(role.getName()) || "SUPER_ADMINISTRATOR".equals(role.getName()));

        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(isAdmin ? "ADMIN" : "USER")
                .build();
    }
}
