package com.familytree.config;

import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.VerificationRequest;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig) so this proves /admin/**
 * genuinely requires ADMIN, not just any authenticated user -- this is
 * privileged tooling (signup approval affects real account access), and
 * before this test there was no explicit /admin/** rule at all, so it
 * would have fallen through to anyRequest().authenticated() and been
 * reachable by ROLE_USER too.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-signups-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminSignupsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private VerificationRequestRepository verificationRequestRepository;

    /**
     * The list page renders fine even when empty (no per-row expressions
     * ever execute); this proves the more complex detail page -- badges,
     * BS date formatting, optional-field conditionals, candidate table --
     * actually renders correctly against real data, not just that the
     * route is reachable.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void detailPageRendersCorrectlyWithRealSubmittedDataAndCandidates() throws Exception {
        Person candidate = new Person();
        candidate.setFirstName("Yuva");
        candidate.setLastName("Bhatta");
        candidate.setGenerationNumber(7);
        candidate = personRepository.save(candidate);

        UserAccount account = new UserAccount();
        account.setEmail("applicant@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account = userAccountRepository.save(account);

        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        request.setSubmittedFatherName("Bhoj Bhatta");
        request.setSubmittedGrandfatherName("Jhanka Bhatta");
        request.setSubmittedDobAd(LocalDate.of(1995, 6, 15));
        request.setSubmittedDobBsYear(2052);
        request.setSubmittedDobBsMonth(2);
        request.setSubmittedDobBsDay(31);
        request.setMotherName("Mina Bhatta");
        request.setMatchConfidence(MatchConfidence.MEDIUM);
        request.setMatchedCandidatePersonIds(String.valueOf(candidate.getId()));
        request = verificationRequestRepository.save(request);

        mockMvc.perform(get("/admin/signups/" + request.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yuva Bhatta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bhoj Bhatta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mina Bhatta")));
    }

    @Test
    void adminSignupsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/signups"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminSignupsIsForbiddenForNonAdminMembers() throws Exception {
        mockMvc.perform(get("/admin/signups"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminSignupsIsAvailableToAdmins() throws Exception {
        mockMvc.perform(get("/admin/signups"))
                .andExpect(status().isOk());
    }
}
