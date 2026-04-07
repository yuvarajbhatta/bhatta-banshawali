package com.familytree.config;

import com.familytree.entity.AppUser;
import com.familytree.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void passwordEncoderEncodesAndMatchesPasswords() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("secret123");

        assertThat(encoded).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", encoded)).isTrue();
    }

    @Test
    void userDetailsServiceLoadsUserFromRepository() {
        AppUserRepository repository = mock(AppUserRepository.class);
        AppUser user = new AppUser();
        user.setUsername("admin");
        user.setPassword("encoded-password");
        user.setRole("ROLE_ADMIN");
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));

        var userDetails = securityConfig.userDetailsService(repository).loadUserByUsername("admin");

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    void userDetailsServiceThrowsWhenUserMissing() {
        AppUserRepository repository = mock(AppUserRepository.class);
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityConfig.userDetailsService(repository).loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: missing");
    }
}
