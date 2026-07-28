package com.familytree.controller;

import com.familytree.dto.MemberProfileDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.RelationshipService;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberProfileControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private Authentication authentication;

    private MemberProfileController controllerWithRealAssembler() {
        PersonProfileAssembler assembler = new PersonProfileAssembler(relationshipService, new PersonDisplayHelper());
        return new MemberProfileController(userAccountRepository, userPersonLinkRepository, assembler);
    }

    @Test
    void returnsUnlinkedProfileWhenAccountHasNoVerifiedPersonLink() {
        when(authentication.getName()).thenReturn("applicant@example.com");
        UserAccount account = new UserAccount();
        account.setEmail("applicant@example.com");
        when(userAccountRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(account));
        when(userPersonLinkRepository.findByUserAccountId(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        MemberProfileDto profile = controllerWithRealAssembler().me(authentication);

        assertThat(profile.linked()).isFalse();
        assertThat(profile.person()).isNull();
        assertThat(profile.family()).isNull();
        assertThat(profile.email()).isEqualTo("applicant@example.com");
    }

    @Test
    void ignoresNonVerifiedLinksWhenResolvingTheMemberProfile() {
        when(authentication.getName()).thenReturn("pending-link@example.com");
        UserAccount account = new UserAccount();
        account.setEmail("pending-link@example.com");
        when(userAccountRepository.findByEmail("pending-link@example.com")).thenReturn(Optional.of(account));

        UserPersonLink pendingLink = new UserPersonLink();
        pendingLink.setLinkStatus(UserPersonLinkStatus.PENDING);
        pendingLink.setPerson(new Person());
        when(userPersonLinkRepository.findByUserAccountId(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(pendingLink));

        MemberProfileDto profile = controllerWithRealAssembler().me(authentication);

        assertThat(profile.linked()).isFalse();
    }

    @Test
    void returnsLinkedProfileWithFamilySnapshotWhenVerified() {
        when(authentication.getName()).thenReturn("member@example.com");
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));

        Person self = new Person();
        self.setId(100L);
        self.setFirstName("Yuva");
        self.setLastName("Bhatta");
        self.setGenerationNumber(8);

        UserPersonLink verifiedLink = new UserPersonLink();
        verifiedLink.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        verifiedLink.setPerson(self);
        when(userPersonLinkRepository.findByUserAccountId(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(verifiedLink));

        Person father = new Person();
        father.setId(101L);
        father.setFirstName("Bhoj");
        Relationship fatherRelationship = new Relationship();
        fatherRelationship.setRelatedPerson(father);
        when(relationshipService.getRelationshipsByPersonAndType(self, RelationshipType.FATHER))
                .thenReturn(List.of(fatherRelationship));
        when(relationshipService.getRelationshipsByPersonAndType(self, RelationshipType.MOTHER))
                .thenReturn(List.of());

        Person child = new Person();
        child.setId(102L);
        child.setFirstName("Kiran");
        when(relationshipService.getSpousesForPerson(self)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(self)).thenReturn(List.of(child));

        MemberProfileDto profile = controllerWithRealAssembler().me(authentication);

        assertThat(profile.linked()).isTrue();
        assertThat(profile.person().id()).isEqualTo(100L);
        assertThat(profile.person().englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(profile.family().father().id()).isEqualTo(101L);
        assertThat(profile.family().mother()).isNull();
        assertThat(profile.family().spouses()).isEmpty();
        assertThat(profile.family().children()).hasSize(1);
        assertThat(profile.family().children().get(0).englishFullName()).isEqualTo("Kiran");
    }

    @Test
    void throws404WhenNoUserAccountExistsForTheAuthenticatedPrincipal() {
        when(authentication.getName()).thenReturn("admin");
        when(userAccountRepository.findByEmail("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controllerWithRealAssembler().me(authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No member profile");
    }
}
