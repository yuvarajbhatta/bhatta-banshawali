package com.familytree.config;

import com.familytree.services.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for the real bug this covers: a user navigating
 * directly to /login (no saved request -- e.g. they didn't get bounced
 * there from a protected page) used to fall back to redirecting to "/"
 * after a successful login. That was fine when "/" was this backend's own
 * home page; it silently became a dead end once nginx started routing "/"
 * (banshawali.yrbhatta.com) to the separate Next.js public landing page.
 *
 * The GET /login "already authenticated" variant of this same regression
 * used to live here too; it no longer applies now that /login itself is
 * a Next.js page with no backend mapping at all (see
 * app/[locale]/login/page.tsx) -- only the credential-processing POST
 * below is still this backend's concern.
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
    void freshLoginWithNoSavedRequestRedirectsToTheDashboardNotRoot() throws Exception {
        appUserService.registerUserIfMissing("regressiontestuser", "password123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .param("username", "regressiontestuser")
                        .param("password", "password123")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    /**
     * Regression test for a second bug this same defaultSuccessUrl(url,
     * false) setting opened up: Spring Security's default
     * HttpSessionRequestCache caches ANY unauthenticated GET, including
     * app/[locale]/login/page.tsx's own "is this visitor already signed
     * in?" SSR check (GET /api/v1/me), not just real page navigations.
     * That check runs whenever /login is reached with an ?error param
     * (a failed login attempt) rather than ?logout -- so failing a login
     * once, then succeeding, replayed the cached /api/v1/me request as
     * the post-login redirect instead of "/dashboard". For a legacy
     * AppUser admin login (no UserAccount to look up), that API call
     * 404s outright, and the browser lands on a raw Whitelabel error
     * page. SecurityConfig now excludes /api/** from the request cache
     * so only real page requests are ever replayed.
     */
    @Test
    void aStrayApiCallInTheSameSessionNeverReplacesTheDefaultSuccessUrl() throws Exception {
        appUserService.registerUserIfMissing("regressiontestuser2", "password123");
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/me").session(session))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .session(session)
                        .param("username", "regressiontestuser2")
                        .param("password", "password123")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
