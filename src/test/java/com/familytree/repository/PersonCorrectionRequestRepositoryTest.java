package com.familytree.repository;

import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:person-correction-request-repo;DB_CLOSE_DELAY=-1",
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
class PersonCorrectionRequestRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PersonCorrectionRequestRepository personCorrectionRequestRepository;

    @Test
    void findByPersonIdReturnsOnlyThatPersonsRequests() {
        Person person = personRepository.save(createPerson("Yuva"));
        Person otherPerson = personRepository.save(createPerson("Bhoj"));
        UserAccount submitter = userAccountRepository.save(createUserAccount("member@example.com"));

        PersonCorrectionRequest forPerson = personCorrectionRequestRepository.save(
                createCorrectionRequest(person, submitter));
        personCorrectionRequestRepository.save(createCorrectionRequest(otherPerson, submitter));

        assertThat(personCorrectionRequestRepository.findByPersonId(person.getId()))
                .containsExactly(forPerson);
    }

    private Person createPerson(String firstName) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName("Bhatta");
        return person;
    }

    private UserAccount createUserAccount(String email) {
        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setPasswordHash("hash");
        return account;
    }

    private PersonCorrectionRequest createCorrectionRequest(Person person, UserAccount submitter) {
        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setSubmittedBy(submitter);
        request.setField(CorrectablePersonField.NICKNAME);
        request.setProposedValue("New Nickname");
        request.setReason("Testing");
        return request;
    }
}
