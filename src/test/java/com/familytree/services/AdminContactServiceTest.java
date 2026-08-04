package com.familytree.services;

import com.familytree.dto.AdminContactDto;
import com.familytree.entity.AppUser;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContactServiceTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    private AdminContactService service() {
        return new AdminContactService(appUserRepository, userAccountRepository);
    }

    private AppUser appUser(String username, String role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    @Test
    void includesLegacyAdminAppUsersButNotPlainUsers() {
        when(appUserRepository.findAll()).thenReturn(List.of(appUser("admin", "ROLE_ADMIN"), appUser("user", "ROLE_USER")));
        when(userAccountRepository.findByRoles_NameIn(List.of("ADMINISTRATOR", "SUPER_ADMINISTRATOR"))).thenReturn(List.of());

        List<AdminContactDto> contacts = service().listAdminContacts();

        assertThat(contacts).containsExactly(new AdminContactDto("admin", null));
    }

    @Test
    void includesUserAccountAdminsByEmail() {
        UserAccount account = new UserAccount();
        account.setEmail("admin@example.com");
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(userAccountRepository.findByRoles_NameIn(List.of("ADMINISTRATOR", "SUPER_ADMINISTRATOR")))
                .thenReturn(List.of(account));

        List<AdminContactDto> contacts = service().listAdminContacts();

        assertThat(contacts).containsExactly(new AdminContactDto("admin@example.com", "admin@example.com"));
    }
}
