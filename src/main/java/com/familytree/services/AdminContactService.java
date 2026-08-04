package com.familytree.services;

import com.familytree.dto.AdminContactDto;
import com.familytree.entity.AppUser;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Who currently has admin access, for the Help & Contact page's "reach
 * an admin" section (see AdminContactController). Checks both admin
 * sources -- today's only admin is a legacy AppUser row (username only,
 * no email on file), UserAccount-based admins (granted via the invite/
 * admin-access-request flow) have a real email.
 */
@Service
public class AdminContactService {

    private static final List<String> ADMIN_ROLE_NAMES = List.of("ADMINISTRATOR", "SUPER_ADMINISTRATOR");

    private final AppUserRepository appUserRepository;
    private final UserAccountRepository userAccountRepository;

    public AdminContactService(AppUserRepository appUserRepository, UserAccountRepository userAccountRepository) {
        this.appUserRepository = appUserRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<AdminContactDto> listAdminContacts() {
        List<AdminContactDto> contacts = new ArrayList<>();

        appUserRepository.findAll().stream()
                .filter(AdminContactService::isAdminAppUser)
                .map(user -> new AdminContactDto(user.getUsername(), null))
                .forEach(contacts::add);

        userAccountRepository.findByRoles_NameIn(ADMIN_ROLE_NAMES).stream()
                .map(account -> new AdminContactDto(account.getEmail(), account.getEmail()))
                .forEach(contacts::add);

        return contacts;
    }

    private static boolean isAdminAppUser(AppUser user) {
        String role = user.getRole() == null ? "" : user.getRole().replace("ROLE_", "");
        return "ADMIN".equals(role);
    }
}
