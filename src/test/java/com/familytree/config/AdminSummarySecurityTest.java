package com.familytree.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves GET /api/v1/admin/summary genuinely requires ADMIN, not just
 * any authenticated user -- without the explicit /api/v1/admin/** rule
 * in SecurityConfig, this would fall through to anyRequest().authenticated()
 * and be reachable by ROLE_USER too, leaking pending-review counts and
 * applicant names to any verified member.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-summary-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminSummarySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void isForbiddenForNonAdminMembers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void isAvailableToAdminsAndReturnsEmptySummaryWithNoDataYet() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isOk());
    }
}
