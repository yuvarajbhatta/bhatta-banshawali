package com.familytree.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageLoads() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // The "already authenticated -> redirect target" regression tests for
    // this same bug live in LoginRedirectTest instead of here: this class
    // uses @AutoConfigureMockMvc(addFilters = false), which doesn't reliably
    // resolve the Authentication method parameter the same way a full
    // security-filter-chain context does.

    // The old unverified /signup self-registration tests were removed along
    // with that flow -- see SignupControllerTest and VerificationReviewServiceTest
    // for the pipeline that replaced it.
}
