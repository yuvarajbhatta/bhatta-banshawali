package com.familytree.config;

import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the real mechanism submitCorrection (frontend/lib/api.ts) relies
 * on: the backend writes an XSRF-TOKEN cookie on every request (not just
 * when a Thymeleaf form reads _csrf -- see CsrfCookieFilter), and echoing
 * that value back in the X-XSRF-TOKEN header on an authenticated POST
 * actually passes CSRF validation, rather than being rejected 403 the
 * way an authenticated call with no CSRF handling at all was before this
 * was wired up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:csrf-cookie-filter;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CsrfCookieFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void anyRequestReceivesAnXsrfTokenCookieEvenWithoutVisitingAThymeleafForm() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        assertThat(xsrfCookie.getValue()).isNotBlank();
    }

    @Test
    void authenticatedPostSucceedsWhenTheXsrfCookieIsEchoedBackInTheHeader() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        account.setPasswordHash("{noop}unused");
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(account);

        Person person = new Person();
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        person = personRepository.save(person);

        MvcResult tokenFetch = mockMvc.perform(get("/actuator/health")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("USER")))
                .andReturn();
        Cookie xsrfCookie = tokenFetch.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();

        mockMvc.perform(post("/api/v1/persons/" + person.getId() + "/corrections")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\":\"NICKNAME\",\"proposedValue\":\"New Nickname\",\"reason\":\"Family calls him this\"}")
                        .cookie(new MockCookie("XSRF-TOKEN", xsrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedPostWithoutTheXsrfHeaderIsRejected() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("noheader@example.com");
        account.setPasswordHash("{noop}unused");
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(account);

        Person person = new Person();
        person.setFirstName("Someone");
        person.setLastName("Else");
        person = personRepository.save(person);

        mockMvc.perform(post("/api/v1/persons/" + person.getId() + "/corrections")
                        .with(SecurityMockMvcRequestPostProcessors.user("noheader@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\":\"NICKNAME\",\"proposedValue\":\"x\",\"reason\":\"y\"}"))
                .andExpect(status().isForbidden());
    }
}
