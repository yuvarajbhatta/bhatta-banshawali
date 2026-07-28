package com.familytree.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for a real bug: an unauthenticated call to a protected
 * /api/** endpoint used to hit formLogin's default entry point, which
 * redirects to /login -- a JSON API caller (the Next.js dashboard's
 * server-to-server fetch to GET /api/v1/me) followed that redirect and
 * got the login page's HTML back with a 200 status, then failed trying
 * to JSON.parse it. API paths must get a real 401 instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:api-auth-entry-point;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ApiAuthenticationEntryPointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedProtectedApiCallReturns401NotARedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedProtectedPageCallStillRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/persons"))
                .andExpect(status().is3xxRedirection());
    }
}
