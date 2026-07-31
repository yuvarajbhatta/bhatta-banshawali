package com.familytree.config;

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
 * Proves POST /persons (PersonController#savePerson, the legacy Thymeleaf
 * create endpoint) genuinely requires ADMIN, not just any authenticated
 * member -- docs/09-security-threat-model.md item 9 (Mass Assignment)
 * finding: the general "/persons" rule in SecurityConfig matched every
 * HTTP method including this one, so any ROLE_USER could create Person
 * rows directly, bypassing the admin-only tooling entirely.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:person-create-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class PersonCreateSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private Cookie fetchXsrfTokenAs(String username, String... roles) throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .with(SecurityMockMvcRequestPostProcessors.user(username).roles(roles)))
                .andReturn();
        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        return xsrfCookie;
    }

    @Test
    void isForbiddenForNonAdminMembers() throws Exception {
        Cookie xsrfCookie = fetchXsrfTokenAs("member@example.com", "USER");

        mockMvc.perform(post("/persons")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("USER"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .param("firstName", "Sneaky")
                        .param("lastName", "Creation"))
                .andExpect(status().isForbidden());
    }

    @Test
    void succeedsForAdmins() throws Exception {
        Cookie xsrfCookie = fetchXsrfTokenAs("admin@example.com", "ADMIN");

        mockMvc.perform(post("/persons")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .param("firstName", "Legit")
                        .param("lastName", "Creation"))
                .andExpect(status().is3xxRedirection());
    }
}
