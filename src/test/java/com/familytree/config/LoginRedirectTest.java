package com.familytree.config;

import com.familytree.services.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for the real bug this covers: a user navigating
 * directly to /login (no saved request -- e.g. they didn't get bounced
 * there from a protected page) used to fall back to redirecting to "/"
 * after a successful login. That was fine when "/" was this backend's own
 * home page; it silently became a dead end once nginx started routing "/"
 * (banshawali.yrbhatta.com) to the separate Next.js public landing page.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:login-redirect;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LoginRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserService appUserService;

    @Test
    void freshLoginWithNoSavedRequestRedirectsToABackendOwnedPathNotRoot() throws Exception {
        appUserService.registerUserIfMissing("regressiontestuser", "password123");

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .param("username", "regressiontestuser")
                        .param("password", "password123")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/persons"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void alreadyAuthenticatedLoginRedirectsToABackendOwnedPathNotRoot() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/persons"));
    }

    // /signup used to be a backend-rendered page with the same
    // already-authenticated redirect as /login above; it was retired in
    // favor of the Next.js verification-based signup flow, so that
    // regression case no longer applies here.
}
