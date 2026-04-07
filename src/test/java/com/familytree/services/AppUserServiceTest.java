package com.familytree.services;

import com.familytree.entity.AppUser;
import com.familytree.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void registerUserSavesEncodedPasswordWithUserRole() {
        when(appUserRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");

        appUserService.registerUser("  newuser  ", "secret123");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("newuser");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-secret");
        assertThat(captor.getValue().getRole()).isEqualTo(AppUserService.ROLE_USER);
    }

    @Test
    void registerUserRejectsDuplicateUsername() {
        when(appUserRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.registerUser("existing", "secret123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already in use.");
    }

    @Test
    void registerUserRejectsBlankUsername() {
        assertThatThrownBy(() -> appUserService.registerUser("   ", "secret123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is required.");
    }

    @Test
    void registerAdminIfMissingCreatesAdmin() {
        when(appUserRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-admin");

        appUserService.registerAdminIfMissing(" admin ", "admin123");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(AppUserService.ROLE_ADMIN);
    }

    @Test
    void registerAdminIfMissingSkipsExistingAdmin() {
        when(appUserRepository.existsByUsername("admin")).thenReturn(true);

        appUserService.registerAdminIfMissing("admin", "admin123");

        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerAdminIfMissingSkipsBlankUsername() {
        appUserService.registerAdminIfMissing("   ", "admin123");

        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerUserIfMissingCreatesUserWhenMissing() {
        when(appUserRepository.existsByUsername("user")).thenReturn(false);
        when(passwordEncoder.encode("user123")).thenReturn("encoded-user");

        appUserService.registerUserIfMissing(" user ", "user123");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("user");
        assertThat(captor.getValue().getRole()).isEqualTo(AppUserService.ROLE_USER);
    }

    @Test
    void registerUserIfMissingSkipsExistingUser() {
        when(appUserRepository.existsByUsername("user")).thenReturn(true);

        appUserService.registerUserIfMissing("user", "user123");

        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
