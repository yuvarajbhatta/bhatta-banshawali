package com.familytree.repository;

import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-person-link-repo;DB_CLOSE_DELAY=-1",
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
class UserPersonLinkRepositoryTest {

    @Autowired
    private UserPersonLinkRepository userPersonLinkRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void findByPersonIdAndFindByUserAccountIdReturnPendingLink() {
        UserAccount account = new UserAccount();
        account.setEmail("applicant@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account = userAccountRepository.save(account);

        Person person = new Person();
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        person = personRepository.save(person);

        UserPersonLink link = new UserPersonLink();
        link.setUserAccount(account);
        link.setPerson(person);
        link = userPersonLinkRepository.save(link);

        assertThat(link.getLinkStatus()).isEqualTo(UserPersonLinkStatus.PENDING);
        assertThat(userPersonLinkRepository.findByPersonId(person.getId())).containsExactly(link);
        assertThat(userPersonLinkRepository.findByUserAccountId(account.getId())).containsExactly(link);
    }

    @Test
    void allowsUnlinkedApplicantWithNoPersonYet() {
        UserAccount account = new UserAccount();
        account.setEmail("no-match@example.com");
        account.setPasswordHash("{bcrypt}hash");
        account = userAccountRepository.save(account);

        UserPersonLink link = new UserPersonLink();
        link.setUserAccount(account);
        userPersonLinkRepository.save(link);

        assertThat(userPersonLinkRepository.findByUserAccountId(account.getId())).hasSize(1);
        assertThat(userPersonLinkRepository.findByUserAccountId(account.getId()).get(0).getPerson()).isNull();
    }
}
