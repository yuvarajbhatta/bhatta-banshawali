package com.familytree.controller;

import com.familytree.services.SignupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig) so this proves POST
 * /api/v1/signup is genuinely reachable without authentication, and --
 * the most important property here -- that the response is identical
 * regardless of what happens internally (new account vs. already
 * registered vs. any match confidence), since SignupService communicates
 * nothing back to the controller by design.
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
        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        verify(signupService).submitSignup(any());
    }

    @Test
    void responseIsIdenticalWhetherSignupIsNewOrTheServiceSilentlyNoOps() throws Exception {
        // The service returns void either way (new account vs. already
        // registered vs. any match confidence) -- nothing for the
        // controller to leak. This asserts the actual response bytes are
        // identical across both cases, not just "some 200".
        MvcResult first = mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(first.getResponse().getContentAsString())
                .isEqualTo(second.getResponse().getContentAsString());
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

        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsWhenTermsNotAgreed() throws Exception {
        String body = VALID_BODY.replace("\"agreedToTerms\": true", "\"agreedToTerms\": false");

        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doesNotCallSignupServiceWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON).content("{}"));

        org.mockito.Mockito.verifyNoInteractions(signupService);
    }
}
