package com.familytree.repository;

import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-account-repo;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Transactional
class UserAccountRepositoryTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findByEmailReturnsSavedAccountWithRoles() {
        Role role = new Role();
        role.setName("VERIFIED_MEMBER");
        role = roleRepository.save(role);

        UserAccount account = new UserAccount();
        account.setEmail("yuva@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account.setStatus(UserAccountStatus.ACTIVE);
        account.setRoles(Set.of(role));
        userAccountRepository.save(account);

        assertThat(userAccountRepository.findByEmail("yuva@example.com"))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
                    assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("VERIFIED_MEMBER");
                });
    }

    @Test
    void existsByEmailDistinguishesKnownAndUnknownAddresses() {
        UserAccount account = new UserAccount();
        account.setEmail("known@example.com");
        account.setPasswordHash("{bcrypt}hash");
        userAccountRepository.save(account);

        assertThat(userAccountRepository.existsByEmail("known@example.com")).isTrue();
        assertThat(userAccountRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    void defaultsToPendingEmailVerificationStatus() {
        UserAccount account = new UserAccount();
        account.setEmail("pending@example.com");
        account.setPasswordHash("{bcrypt}hash");
        UserAccount saved = userAccountRepository.save(account);

        assertThat(saved.getStatus()).isEqualTo(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
    }
}
