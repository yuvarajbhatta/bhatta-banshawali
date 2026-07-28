package com.familytree.controller;

import com.familytree.dto.FamilySnapshotDto;
import com.familytree.dto.MemberProfileDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.entity.RelationshipType;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.RelationshipService;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * The logged-in member's own dashboard data (docs/08 Phase 4). Only
 * meaningful for a UserAccount-based login (a verified applicant) --
 * the old AppUser admins have no UserAccount row and no Person link at
 * all, so this 404s for them rather than returning an empty profile,
 * since "not a member profile" and "member profile with nothing linked
 * yet" are different states (see MemberProfileDto.unlinked).
 */
@RestController
@RequestMapping("/api/v1/me")
public class MemberProfileController {

    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final RelationshipService relationshipService;
    private final PersonDisplayHelper personDisplay;

    public MemberProfileController(UserAccountRepository userAccountRepository,
                                   UserPersonLinkRepository userPersonLinkRepository,
                                   RelationshipService relationshipService,
                                   PersonDisplayHelper personDisplay) {
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.relationshipService = relationshipService;
        this.personDisplay = personDisplay;
    }

    @GetMapping
    public MemberProfileDto me(Authentication authentication) {
        UserAccount account = userAccountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No member profile for this account."));

        Person person = userPersonLinkRepository.findByUserAccountId(account.getId()).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .map(UserPersonLink::getPerson)
                .findFirst()
                .orElse(null);

        if (person == null) {
            return MemberProfileDto.unlinked(account.getEmail());
        }

        return new MemberProfileDto(account.getEmail(), true, summarize(person), familySnapshot(person));
    }

    private FamilySnapshotDto familySnapshot(Person person) {
        PersonSummaryDto father = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)
                .stream().findFirst().map(r -> summarize(r.getRelatedPerson())).orElse(null);
        PersonSummaryDto mother = relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER)
                .stream().findFirst().map(r -> summarize(r.getRelatedPerson())).orElse(null);
        List<PersonSummaryDto> spouses = relationshipService.getSpousesForPerson(person).stream()
                .map(this::summarize).toList();
        List<PersonSummaryDto> children = relationshipService.getChildrenForPerson(person).stream()
                .map(this::summarize).toList();

        return new FamilySnapshotDto(father, mother, spouses, children);
    }

    private PersonSummaryDto summarize(Person person) {
        return new PersonSummaryDto(
                person.getId(),
                personDisplay.englishFullName(person),
                personDisplay.nepaliFullName(person),
                person.getGenerationNumber(),
                person.getGender(),
                person.getBirthDate()
        );
    }
}
