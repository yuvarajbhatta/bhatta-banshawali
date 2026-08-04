package com.familytree.config;

import com.familytree.entity.UserAccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * UserDetails for a UserAccount-backed login, carrying its
 * UserAccountStatus alongside what Spring Security normally cares about.
 * Always reports enabled/unlocked/non-expired regardless of the real
 * status -- the actual status gate lives in UserAccountStatusChecker (a
 * postAuthenticationChecks UserDetailsChecker), which only runs AFTER the
 * password has already been verified correct. That ordering is the whole
 * point: a non-ACTIVE account's status is never revealed to someone who
 * doesn't already know the password, preserving the anti-enumeration
 * guarantee that BridgingUserDetailsService's old "filter to ACTIVE only,
 * throw UsernameNotFoundException otherwise" approach gave for free but
 * at the cost of a pending applicant with the *correct* password getting
 * the same unhelpful "invalid credentials" as a wrong guess.
 */
public class UserAccountPrincipal implements UserDetails {

    private final String email;
    private final String passwordHash;
    private final UserAccountStatus status;
    private final List<GrantedAuthority> authorities;

    public UserAccountPrincipal(String email, String passwordHash, UserAccountStatus status, List<String> roles) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.authorities = roles.stream().<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    public UserAccountStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
