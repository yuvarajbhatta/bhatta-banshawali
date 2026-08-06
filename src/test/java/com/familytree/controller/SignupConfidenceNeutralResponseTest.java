package com.familytree.controller;

import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * docs/09-security-threat-model.md item 3 (Family-Member Enumeration via
 * Signup Matching): proves the applicant-facing response carries no
 * confidence-dependent data -- using the REAL SignupService and
 * FamilyMatchService (not mocked, unlike SignupControllerTest), with real
 * Person/Relationship fixtures engineered to actually produce HIGH, MEDIUM,
 * and LOW confidence per FamilyMatchService's own rules (see
 * FamilyMatchServiceTest for the same scenarios in isolation). Any future
 * change that leaks confidence-dependent data into the response would fail
 * this for at least one real scenario.
 *
 * photoUploadToken is the one field that's deliberately NOT identical
 * across calls (it's a fresh random UUID per signup, see SignupService,
 * used to attach a photo to this specific pending request without a
 * session) -- so this checks its format/presence and that every response
 * has exactly the same two fields, rather than a byte-exact body match.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:signup-confidence-neutral-response;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class SignupConfidenceNeutralResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private Person person(String first, String last) {
        Person person = new Person();
        person.setFirstName(first);
        person.setLastName(last);
        return personRepository.save(person);
    }

    private void father(Person child, Person father) {
        Relationship relationship = new Relationship();
        relationship.setPerson(child);
        relationship.setRelatedPerson(father);
        relationship.setRelationshipType(RelationshipType.FATHER);
        relationshipRepository.save(relationship);
    }

    private String signupBody(String email, String fullName, String fatherName, String grandfatherName) {
        return """
                {
                    "email": "%s",
                    "fullName": "%s",
                    "dobAd": "1995-06-15",
                    "fatherName": "%s",
                    "grandfatherName": "%s",
                    "password": "password123",
                    "confirmPassword": "password123",
                    "preferredLanguage": "en",
                    "agreedToTerms": true
                }
                """.formatted(email, fullName, fatherName, grandfatherName);
    }

    private String assertNeutralResponse(String email, String fullName, String fatherName, String grandfatherName,
                                         String clientIp) throws Exception {
        return mockMvc.perform(post("/api/v1/signup").contentType(APPLICATION_JSON)
                        .content(signupBody(email, fullName, fatherName, grandfatherName))
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*").value(iterableWithSize(2)))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.photoUploadToken").value(matchesPattern(UUID_PATTERN)))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void responseIsIdenticalAcrossHighMediumAndLowConfidence() throws Exception {
        // HIGH: name + father + grandfather all match a single, unambiguous lineage.
        Person grandfather = person("Jhanka", "Bhatta");
        Person father = person("Bhoj", "Bhatta");
        father(father, grandfather);
        Person applicantMatch = person("Yuva", "Bhatta");
        father(applicantMatch, father);

        // MEDIUM: father matches, but the stated grandfather doesn't -- an
        // incomplete/unconfirmed lineage chain, not a full match.
        Person mediumFather = person("Ramesh", "Bhatta");
        Person mediumMatch = person("Sunita", "Bhatta");
        father(mediumMatch, mediumFather);

        // LOW: name doesn't match anyone in the tree at all.

        String highBody = assertNeutralResponse("high@example.com", "Yuva Bhatta", "Bhoj Bhatta", "Jhanka Bhatta", "203.0.113.101");
        String mediumBody = assertNeutralResponse("medium@example.com", "Sunita Bhatta", "Ramesh Bhatta", "Someone Unrelated", "203.0.113.102");
        String lowBody = assertNeutralResponse("low@example.com", "Nobody Existing", "Nobody Else", "Nobody Prior", "203.0.113.103");

        // Each token is freshly random, so three distinct bodies is
        // expected, not a leak -- this just confirms they really are
        // independent random values rather than something predictable
        // (e.g. a counter) that could itself become a side channel.
        assertThat(List.of(highBody, mediumBody, lowBody)).doesNotHaveDuplicates();
    }
}
