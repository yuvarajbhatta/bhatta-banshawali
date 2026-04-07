package com.familytree.config;

import com.familytree.services.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminUserInitializer {

    @Bean
    public CommandLineRunner adminInitializer(AppUserService appUserService, AppProperties appProperties) {
        return args -> {
            String adminUsername = appProperties.getAdmin().getUsername();
            String adminPassword = appProperties.getAdmin().getPassword();

            if (adminUsername == null || adminUsername.isBlank()
                    || adminPassword == null || adminPassword.isBlank()) {
                return;
            }

            appUserService.registerAdminIfMissing(adminUsername.trim(), adminPassword);
        };
    }
}
