package com.familytree.config;

import com.familytree.services.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevUserInitializer {

    @Bean
    public CommandLineRunner devUserSeedRunner(AppUserService appUserService, AppProperties appProperties) {
        return args -> {
            String username = appProperties.getDevUser().getUsername();
            String password = appProperties.getDevUser().getPassword();

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return;
            }

            appUserService.registerUserIfMissing(username.trim(), password);
        };
    }
}
