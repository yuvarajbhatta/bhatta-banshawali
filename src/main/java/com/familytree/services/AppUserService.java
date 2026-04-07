package com.familytree.services;

import com.familytree.entity.AppUser;
import com.familytree.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(String username, String rawPassword){
        registerUserWithRole(username, rawPassword, ROLE_USER);
    }

    public void registerAdminIfMissing(String username, String rawPassword) {
        registerUserWithRoleIfMissing(username, rawPassword, ROLE_ADMIN);
    }

    public void registerUserIfMissing(String username, String rawPassword) {
        registerUserWithRoleIfMissing(username, rawPassword, ROLE_USER);
    }

    private void registerUserWithRole(String username, String rawPassword, String role) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (appUserRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username is already in use.");
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        appUserRepository.save(user);
    }

    private void registerUserWithRoleIfMissing(String username, String rawPassword, String role) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty() || appUserRepository.existsByUsername(normalizedUsername)) {
            return;
        }
        registerUserWithRole(normalizedUsername, rawPassword, role);
    }
}
