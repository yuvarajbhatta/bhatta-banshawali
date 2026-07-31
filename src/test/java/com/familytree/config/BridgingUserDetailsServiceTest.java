package com.familytree.config;

import com.familytree.entity.AppUser;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BridgingUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private BridgingUserDetailsService bridgingUserDetailsService;

    @Test
    void loadsFromAppUserWhenPresent() {
        AppUser user = new AppUser();
        user.setUsername("admin");
        user.setPassword("encoded-password");
        user.setRole("ROLE_ADMIN");
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails userDetails = bridgingUserDetailsService.loadUserByUsername("admin");

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        // The legacy AppUser admin is the site owner's account and predates the
        // whole verification workflow -- it gets SUPER_ADMIN too so it's never
        // the one account that could get locked out of super-admin-gated
        // actions (docs/09-security-threat-model.md item 13).
        assertThat(userDetails.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    @Test
    void legacyAppUserWithPlainUserRoleGetsNoSuperAdmin() {
        AppUser user = new AppUser();
        user.setUsername("member");
        user.setPassword("encoded-password");
        user.setRole("ROLE_USER");
        when(appUserRepository.findByUsername("member")).thenReturn(Optional.of(user));

        UserDetails userDetails = bridgingUserDetailsService.loadUserByUsername("member");

        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void doesNotConsultUserAccountRepositoryWhenAppUserFound() {
        AppUser user = new AppUser();
        user.setUsername("admin");
        user.setPassword("encoded-password");
        user.setRole("ROLE_ADMIN");
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        bridgingUserDetailsService.loadUserByUsername("admin");

        org.mockito.Mockito.verifyNoInteractions(userAccountRepository);
    }

    @Test
    void fallsBackToActiveUserAccountByEmailWithVerifiedMemberRole() {
        when(appUserRepository.findByUsername("applicant@example.com")).thenReturn(Optional.empty());
        Role verifiedMember = new Role();
        verifiedMember.setName("VERIFIED_MEMBER");
        UserAccount account = new UserAccount();
        account.setEmail("applicant@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account.setStatus(UserAccountStatus.ACTIVE);
        account.setRoles(Set.of(verifiedMember));
        when(userAccountRepository.findByEmailWithRoles("applicant@example.com")).thenReturn(Optional.of(account));

        UserDetails userDetails = bridgingUserDetailsService.loadUserByUsername("applicant@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("applicant@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void administratorRoleMapsToRoleAdmin() {
        when(appUserRepository.findByUsername("admin@example.com")).thenReturn(Optional.empty());
        Role administrator = new Role();
        administrator.setName("ADMINISTRATOR");
        UserAccount account = new UserAccount();
        account.setEmail("admin@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account.setStatus(UserAccountStatus.ACTIVE);
        account.setRoles(Set.of(administrator));
        when(userAccountRepository.findByEmailWithRoles("admin@example.com")).thenReturn(Optional.of(account));

        UserDetails userDetails = bridgingUserDetailsService.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void superAdministratorRoleMapsToBothRoleAdminAndRoleSuperAdmin() {
        when(appUserRepository.findByUsername("super@example.com")).thenReturn(Optional.empty());
        Role superAdministrator = new Role();
        superAdministrator.setName("SUPER_ADMINISTRATOR");
        UserAccount account = new UserAccount();
        account.setEmail("super@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account.setStatus(UserAccountStatus.ACTIVE);
        account.setRoles(Set.of(superAdministrator));
        when(userAccountRepository.findByEmailWithRoles("super@example.com")).thenReturn(Optional.of(account));

        UserDetails userDetails = bridgingUserDetailsService.loadUserByUsername("super@example.com");

        assertThat(userDetails.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    @Test
    void pendingUserAccountIsTreatedAsNotFound() {
        when(appUserRepository.findByUsername("pending@example.com")).thenReturn(Optional.empty());
        UserAccount account = new UserAccount();
        account.setEmail("pending@example.com");
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        when(userAccountRepository.findByEmailWithRoles("pending@example.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> bridgingUserDetailsService.loadUserByUsername("pending@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void disabledUserAccountIsTreatedAsNotFound() {
        when(appUserRepository.findByUsername("disabled@example.com")).thenReturn(Optional.empty());
        UserAccount account = new UserAccount();
        account.setEmail("disabled@example.com");
        account.setStatus(UserAccountStatus.DISABLED);
        when(userAccountRepository.findByEmailWithRoles("disabled@example.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> bridgingUserDetailsService.loadUserByUsername("disabled@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void neitherAppUserNorUserAccountThrowsGenericUsernameNotFound() {
        when(appUserRepository.findByUsername("nobody@example.com")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailWithRoles("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bridgingUserDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void normalizesEmailCaseAndWhitespaceWhenLookingUpUserAccount() {
        when(appUserRepository.findByUsername("  Applicant@Example.com  ")).thenReturn(Optional.empty());
        UserAccount account = new UserAccount();
        account.setEmail("applicant@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account.setStatus(UserAccountStatus.ACTIVE);
        when(userAccountRepository.findByEmailWithRoles("applicant@example.com")).thenReturn(Optional.of(account));

        UserDetails userDetails = bridgingUserDetailsService.loadUserByUsername("  Applicant@Example.com  ");

        assertThat(userDetails.getUsername()).isEqualTo("applicant@example.com");
    }
}
