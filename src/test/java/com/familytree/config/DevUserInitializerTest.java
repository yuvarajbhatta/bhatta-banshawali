package com.familytree.config;

import com.familytree.services.AppUserService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DevUserInitializerTest {

    private final DevUserInitializer initializer = new DevUserInitializer();

    @Test
    void createsDevUserWhenCredentialsArePresent() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getDevUser().setUsername("user");
        appProperties.getDevUser().setPassword("user123");

        initializer.devUserSeedRunner(appUserService, appProperties).run();

        verify(appUserService).registerUserIfMissing("user", "user123");
    }

    @Test
    void skipsDevUserCreationWhenCredentialsAreMissing() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        AppProperties appProperties = new AppProperties();

        initializer.devUserSeedRunner(appUserService, appProperties).run();

        verifyNoInteractions(appUserService);
    }
}
