package com.familytree.config;

import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void passwordEncoderEncodesAndMatchesPasswords() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("secret123");

        assertThat(encoded).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", encoded)).isTrue();
    }

    // The actual authentication behavior (AppUser vs UserAccount, role
    // mapping, inactive-account handling) is covered by
    // BridgingUserDetailsServiceTest -- this just confirms the bean wires
    // up to that implementation rather than duplicating those cases here.
    @Test
    void userDetailsServiceDelegatesToBridgingUserDetailsService() {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);

        var userDetailsService = securityConfig.userDetailsService(appUserRepository, userAccountRepository);

        assertThat(userDetailsService).isInstanceOf(BridgingUserDetailsService.class);
    }
}
