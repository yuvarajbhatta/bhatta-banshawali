package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The single guarded implementation behind every place that creates a
 * VERIFIED UserPersonLink -- UserAccountAdminService.link() (manual admin
 * linking) and VerificationReviewService.approve() (signup-approval
 * linking) -- so the duplicate-link guard can never be bypassed by a new
 * caller that forgets it.
 */
@Service
public class UserPersonLinkService {

    private final UserPersonLinkRepository userPersonLinkRepository;
    private final PersonDisplayHelper personDisplay;

    public UserPersonLinkService(UserPersonLinkRepository userPersonLinkRepository, PersonDisplayHelper personDisplay) {
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.personDisplay = personDisplay;
    }

    /**
     * @throws IllegalArgumentException if the account already has a VERIFIED link (unlink
     *          it first), or the target Person is already VERIFIED-linked to a different
     *          account -- otherwise picking the wrong same-named candidate (e.g. one of
     *          several "Bhojraj Bhatta" records) succeeds silently with no way to notice.
     */
    @Transactional
    public UserPersonLink createVerifiedLink(UserAccount account, Person person) {
        boolean accountAlreadyLinked = userPersonLinkRepository.findByUserAccountId(account.getId()).stream()
                .anyMatch(existingLink -> existingLink.getLinkStatus() == UserPersonLinkStatus.VERIFIED);
        if (accountAlreadyLinked) {
            throw new IllegalArgumentException("This account is already linked to a person. Unlink it first.");
        }

        boolean personLinkedToSomeoneElse = userPersonLinkRepository.findByPersonId(person.getId()).stream()
                .anyMatch(existingLink -> existingLink.getLinkStatus() == UserPersonLinkStatus.VERIFIED);
        if (personLinkedToSomeoneElse) {
            throw new IllegalArgumentException(
                    personDisplay.englishFullName(person) + " is already linked to another account.");
        }

        UserPersonLink link = new UserPersonLink();
        link.setUserAccount(account);
        link.setPerson(person);
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        link.setVerifiedAt(LocalDateTime.now());
        return userPersonLinkRepository.save(link);
    }
}
