package com.familytree.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression-proofs docs/09-security-threat-model.md item 14 (Audit-Log
 * Tampering): the append-only guarantee holds only because
 * AdminAuditLogApiController never defines a DELETE/PUT mapping at all --
 * true by construction today, but nothing previously caught a future PR
 * accidentally adding one. Authenticated as ADMIN with a real CSRF token
 * (both DELETE and PUT are state-changing, so a request with no/invalid
 * token would 403 on CSRF grounds before ever reaching routing -- that's
 * not the guarantee this test is checking) so a 404 here means "no such
 * route", not "rejected for an unrelated reason".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-audit-log-mutation-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminAuditLogMutationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private Cookie fetchXsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
                .andReturn();
        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        return xsrfCookie;
    }

    @Test
    void noDeleteRouteExistsForAuditLogEntries() throws Exception {
        Cookie xsrfCookie = fetchXsrfToken();

        mockMvc.perform(delete("/api/v1/admin/audit-log/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isNotFound());
    }

    @Test
    void noUpdateRouteExistsForAuditLogEntries() throws Exception {
        Cookie xsrfCookie = fetchXsrfToken();

        mockMvc.perform(put("/api/v1/admin/audit-log/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isNotFound());
    }
}
