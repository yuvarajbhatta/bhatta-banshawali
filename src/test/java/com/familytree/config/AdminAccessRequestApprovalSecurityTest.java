package com.familytree.config;

import com.familytree.entity.AdminAccessRequest;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AdminAccessRequestRepository;
import com.familytree.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves POST /api/v1/admin/admin-access-requests/{id}/approve genuinely
 * requires SUPER_ADMIN, not just ADMIN -- docs/09-security-threat-model.md
 * item 13 (Admin Privilege Escalation). Without the explicit
 * "hasRole('SUPER_ADMIN')" rule in SecurityConfig (more specific than, and
 * ordered before, the general "/api/v1/admin/**" -> ADMIN rule), any plain
 * ADMINISTRATOR could approve this request and mint another admin account
 * with no further checkpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-access-request-approval-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class AdminAccessRequestApprovalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AdminAccessRequestRepository adminAccessRequestRepository;

    private Long seedPendingRequest() {
        UserAccount account = new UserAccount();
        account.setEmail("applicant@example.com");
        account.setPasswordHash("hash");
        userAccountRepository.save(account);

        AdminAccessRequest request = new AdminAccessRequest();
        request.setUserAccount(account);
        return adminAccessRequestRepository.save(request).getId();
    }

    private Cookie fetchXsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .with(SecurityMockMvcRequestPostProcessors.user("reviewer@example.com").roles("ADMIN", "SUPER_ADMIN")))
                .andReturn();
        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        return xsrfCookie;
    }

    @Test
    void requiresAuthentication() throws Exception {
        Long id = seedPendingRequest();
        // A valid CSRF token but no authenticated principal -- CsrfCookieFilter
        // writes the XSRF-TOKEN cookie for anonymous callers too, so a real
        // unauthenticated request would already have one; supplying it here
        // isolates the assertion to the authentication check, not CSRF.
        MvcResult anonymousTokenFetch = mockMvc.perform(get("/actuator/health")).andReturn();
        Cookie xsrfCookie = anonymousTokenFetch.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();

        mockMvc.perform(post("/api/v1/admin/admin-access-requests/" + id + "/approve")
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void isForbiddenForNonAdminMembers() throws Exception {
        Long id = seedPendingRequest();
        Cookie xsrfCookie = fetchXsrfToken();

        mockMvc.perform(post("/api/v1/admin/admin-access-requests/" + id + "/approve")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("USER"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void isForbiddenForPlainAdminsWithoutSuperAdmin() throws Exception {
        Long id = seedPendingRequest();
        Cookie xsrfCookie = fetchXsrfToken();

        mockMvc.perform(post("/api/v1/admin/admin-access-requests/" + id + "/approve")
                        .with(SecurityMockMvcRequestPostProcessors.user("plain-admin@example.com").roles("ADMIN"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void succeedsForSuperAdmins() throws Exception {
        Long id = seedPendingRequest();
        Cookie xsrfCookie = fetchXsrfToken();

        mockMvc.perform(post("/api/v1/admin/admin-access-requests/" + id + "/approve")
                        .with(SecurityMockMvcRequestPostProcessors.user("reviewer@example.com").roles("ADMIN", "SUPER_ADMIN"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }
}
