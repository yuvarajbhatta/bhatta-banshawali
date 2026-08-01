package com.familytree.controller;

import com.familytree.dto.MemberProfileDto;
import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.ViewerContext;
import com.familytree.services.ViewerContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    private final PersonProfileAssembler personProfileAssembler;
    private final ViewerContextResolver viewerContextResolver;

    public MemberProfileController(UserAccountRepository userAccountRepository,
                                   UserPersonLinkRepository userPersonLinkRepository,
                                   PersonProfileAssembler personProfileAssembler,
                                   ViewerContextResolver viewerContextResolver) {
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.personProfileAssembler = personProfileAssembler;
        this.viewerContextResolver = viewerContextResolver;
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

        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        return new MemberProfileDto(account.getEmail(), true,
                personProfileAssembler.summarize(person, viewer), personProfileAssembler.familySnapshot(person, viewer),
                personProfileAssembler.ancestorChain(person, viewer));
    }
}
