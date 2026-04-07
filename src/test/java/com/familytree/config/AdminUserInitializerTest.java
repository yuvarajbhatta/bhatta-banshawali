package com.familytree.config;

import com.familytree.services.AppUserService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminUserInitializerTest {

    private final AdminUserInitializer initializer = new AdminUserInitializer();

    @Test
    void createsAdminWhenCredentialsArePresent() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getAdmin().setUsername("admin");
        appProperties.getAdmin().setPassword("secret123");

        initializer.adminInitializer(appUserService, appProperties).run();

        verify(appUserService).registerAdminIfMissing("admin", "secret123");
    }

    @Test
    void skipsAdminCreationWhenCredentialsAreMissing() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        AppProperties appProperties = new AppProperties();

        initializer.adminInitializer(appUserService, appProperties).run();

        verifyNoInteractions(appUserService);
    }
}
