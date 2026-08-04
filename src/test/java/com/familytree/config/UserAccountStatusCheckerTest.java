package com.familytree.config;

import com.familytree.entity.UserAccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountStatusCheckerTest {

    private final UserAccountStatusChecker checker = new UserAccountStatusChecker();

    @Test
    void passesForAnActiveUserAccountPrincipal() {
        UserAccountPrincipal principal =
                new UserAccountPrincipal("member@example.com", "{bcrypt}hash", UserAccountStatus.ACTIVE, List.of("USER"));

        assertThatCode(() -> checker.check(principal)).doesNotThrowAnyException();
    }

    @Test
    void throwsAccountNotActiveExceptionCarryingTheStatusForANonActivePrincipal() {
        UserAccountPrincipal principal = new UserAccountPrincipal(
                "pending@example.com", "{bcrypt}hash", UserAccountStatus.PENDING_EMAIL_VERIFICATION, List.of("USER"));

        assertThatThrownBy(() -> checker.check(principal))
                .isInstanceOf(AccountNotActiveException.class)
                .extracting(ex -> ((AccountNotActiveException) ex).getStatus())
                .isEqualTo(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
    }

    @Test
    void passesForAPlainUserDetailsWithNoStatusConcept() {
        // The legacy AppUser-backed principal -- no UserAccountStatus at
        // all, so there's nothing for this checker to gate on.
        UserDetails legacyAdmin = User.withUsername("admin").password("{bcrypt}hash").roles("ADMIN").build();

        assertThatCode(() -> checker.check(legacyAdmin)).doesNotThrowAnyException();
    }
}
