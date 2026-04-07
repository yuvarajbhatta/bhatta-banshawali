package com.familytree.repository;

import com.familytree.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appuser-repo;DB_CLOSE_DELAY=-1",
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
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void findByUsernameReturnsSavedUser() {
        AppUser user = new AppUser();
        user.setUsername("yuva");
        user.setPassword("encoded");
        user.setRole("ROLE_USER");
        appUserRepository.save(user);

        assertThat(appUserRepository.findByUsername("yuva"))
                .hasValueSatisfying(savedUser -> assertThat(savedUser.getRole()).isEqualTo("ROLE_USER"));
    }

    @Test
    void existsByUsernameReturnsTrueForSavedUser() {
        AppUser user = new AppUser();
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setRole("ROLE_ADMIN");
        appUserRepository.save(user);

        assertThat(appUserRepository.existsByUsername("admin")).isTrue();
        assertThat(appUserRepository.existsByUsername("missing")).isFalse();
    }
}
