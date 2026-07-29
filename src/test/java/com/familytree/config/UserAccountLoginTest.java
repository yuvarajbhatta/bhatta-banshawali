package com.familytree.config;

import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the entire point of BridgingUserDetailsService end-to-end: a
 * UserAccount approved through the admin review flow can actually
 * authenticate here. Posts to /api/v1/auth/login (loginProcessingUrl),
 * not /login -- that path is now a Next.js page with no backend mapping
 * at all (see app/[locale]/login/page.tsx).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:useraccount-login;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserAccountLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void activeVerifiedMemberAccountCanLogInAndReachesDashboard() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("approved-applicant@example.com");
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(account);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .param("username", "approved-applicant@example.com")
                        .param("password", "password123")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void administratorRoleGrantsAdminAccess() throws Exception {
        Role administrator = new Role();
        administrator.setName("ADMINISTRATOR");
        administrator = roleRepository.save(administrator);

        UserAccount account = new UserAccount();
        account.setEmail("admin-applicant@example.com");
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setStatus(UserAccountStatus.ACTIVE);
        account.setRoles(Set.of(administrator));
        userAccountRepository.save(account);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .param("username", "admin-applicant@example.com")
                        .param("password", "password123")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/signups")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin-applicant@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void pendingAccountCannotLogIn() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("pending-applicant@example.com");
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        userAccountRepository.save(account);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .param("username", "pending-applicant@example.com")
                        .param("password", "password123")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("disabled-applicant@example.com");
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setStatus(UserAccountStatus.DISABLED);
        userAccountRepository.save(account);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .param("username", "disabled-applicant@example.com")
                        .param("password", "password123")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginIsCaseInsensitiveOnEmailMatchingHowSignupNormalizesIt() throws Exception {
        UserAccount account = new UserAccount();
        account.setEmail("mixedcase@example.com");
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(account);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .param("username", "  MixedCase@Example.com  ")
                        .param("password", "password123")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
