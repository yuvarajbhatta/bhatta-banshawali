package com.familytree.controller;

import com.familytree.services.EmailAlreadyRegisteredException;
import com.familytree.services.SignupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig) so this proves POST
 * /api/v1/signup is genuinely reachable without authentication. Signup
 * deliberately reports a duplicate email back to the caller (see
 * EmailAlreadyRegisteredException's javadoc) -- unlike login and
 * password-reset, which still return constant-shape responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:signup-controller;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignupService signupService;

    private static final String VALID_BODY = """
            {
                "email": "yuva@example.com",
                "fullName": "Yuva Bhatta",
                "dobAd": "1995-06-15",
                "fatherName": "Bhoj Bhatta",
                "grandfatherName": "Jhanka Bhatta",
                "password": "password123",
                "confirmPassword": "password123",
                "preferredLanguage": "en",
                "agreedToTerms": true
            }
            """;

    @Test
    void signupSucceedsWithoutAuthenticationAndReturnsNeutralStatus() throws Exception {
        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(VALID_BODY)
                        .header("X-Forwarded-For", "203.0.113.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        verify(signupService).submitSignup(any());
    }

    @Test
    void returnsConflictWhenEmailAlreadyRegistered() throws Exception {
        doThrow(new EmailAlreadyRegisteredException("An account with this email already exists."))
                .when(signupService).submitSignup(any());

        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(VALID_BODY)
                        .header("X-Forwarded-For", "203.0.113.2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    void rejectsMismatchedPasswords() throws Exception {
        String body = """
                {
                    "email": "yuva@example.com",
                    "fullName": "Yuva Bhatta",
                    "dobAd": "1995-06-15",
                    "fatherName": "Bhoj Bhatta",
                    "grandfatherName": "Jhanka Bhatta",
                    "password": "password123",
                    "confirmPassword": "different123",
                    "preferredLanguage": "en",
                    "agreedToTerms": true
                }
                """;

        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(body)
                        .header("X-Forwarded-For", "203.0.113.3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content("{}")
                        .header("X-Forwarded-For", "203.0.113.4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsWhenTermsNotAgreed() throws Exception {
        String body = VALID_BODY.replace("\"agreedToTerms\": true", "\"agreedToTerms\": false");

        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(body)
                        .header("X-Forwarded-For", "203.0.113.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doesNotCallSignupServiceWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content("{}")
                        .header("X-Forwarded-For", "203.0.113.6"));

        org.mockito.Mockito.verifyNoInteractions(signupService);
    }
}
