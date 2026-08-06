package com.familytree.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves GET /persons/new genuinely requires ADMIN. Without ordering the
 * ADMIN-only "/persons/new" matcher before the general "/persons", "/persons/*"
 * -> ADMIN-or-USER rule in SecurityConfig, the single-path-segment wildcard
 * "/persons/*" matches "/persons/new" too and (first-match-wins) swallows it,
 * silently letting any plain USER load the admin create-person form.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:persons-new-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PersonsNewSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void isForbiddenForPlainUsers() throws Exception {
        mockMvc.perform(get("/persons/new")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void isAllowedForAdmins() throws Exception {
        mockMvc.perform(get("/persons/new")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
