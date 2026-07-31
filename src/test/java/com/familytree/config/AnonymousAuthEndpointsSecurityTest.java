package com.familytree.config;

import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Forgot-password and email-verification-confirm are anonymous,
 * pre-session endpoints, matching signup's treatment: CSRF-exempt and
 * permitAll in SecurityConfig. Each test uses its own X-Forwarded-For
 * value -- these paths are rate-limited (RateLimitFilter), and the
 * RateLimiter bean's in-memory bucket state persists across test methods
 * within this same Spring context (only the database resets via
 * @Transactional), so reusing an IP across tests here would make later
 * tests flakily see 429 instead of the status they're actually asserting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:anonymous-auth-endpoints-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class AnonymousAuthEndpointsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void passwordResetRequestForUnknownEmailReturns404WithClearMessage() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset/request")
                        .header("X-Forwarded-For", "203.0.113.101")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"message\":\"No account found with that email.\"}"));
    }

    @Test
    void passwordResetRequestForKnownEmailReturns200() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("known@example.com");
        account.setPasswordHash("hash");
        userAccountRepository.save(account);

        mockMvc.perform(post("/api/v1/password-reset/request")
                        .header("X-Forwarded-For", "203.0.113.102")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"known@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"RESET_EMAIL_SENT\"}"));
    }

    @Test
    void passwordResetRequestNeedsNoCsrfToken() throws Exception {
        // Zero XSRF-TOKEN cookie/header at all -- a 403 here would mean
        // the CSRF exemption in SecurityConfig didn't take effect. A 404
        // (not-found email) proves the request reached the controller.
        mockMvc.perform(post("/api/v1/password-reset/request")
                        .header("X-Forwarded-For", "203.0.113.103")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordResetConfirmWithGarbageTokenReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset/confirm")
                        .header("X-Forwarded-For", "203.0.113.104")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"garbage\",\"newPassword\":\"newPassword123\",\"confirmNewPassword\":\"newPassword123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailConfirmWithGarbageTokenReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/verify-email/confirm")
                        .header("X-Forwarded-For", "203.0.113.105")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"garbage\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailConfirmNeedsNoCsrfToken() throws Exception {
        // Same reasoning as passwordResetRequestNeedsNoCsrfToken -- zero
        // CSRF token, a 400 (not 403) proves the exemption took effect.
        mockMvc.perform(post("/api/v1/verify-email/confirm")
                        .header("X-Forwarded-For", "203.0.113.106")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"garbage\"}"))
                .andExpect(status().isBadRequest());
    }
}
