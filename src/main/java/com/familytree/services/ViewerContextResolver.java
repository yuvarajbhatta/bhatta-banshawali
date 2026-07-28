package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Builds a ViewerContext from the current Authentication -- shared by
 * MemberProfileController and PersonApiController so "am I an admin,
 * and which Person (if any) am I verified as" isn't resolved twice.
 */
@Service
public class ViewerContextResolver {

    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;

    public ViewerContextResolver(UserAccountRepository userAccountRepository,
                                 UserPersonLinkRepository userPersonLinkRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
    }

    public ViewerContext resolve(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        Long viewerPersonId = userAccountRepository.findByEmail(authentication.getName())
                .map(UserAccount::getId)
                .map(this::findVerifiedPersonId)
                .orElse(null);

        return new ViewerContext(isAdmin, viewerPersonId);
    }

    private Long findVerifiedPersonId(Long userAccountId) {
        return userPersonLinkRepository.findByUserAccountId(userAccountId).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .map(UserPersonLink::getPerson)
                .map(Person::getId)
                .findFirst()
                .orElse(null);
    }
}
