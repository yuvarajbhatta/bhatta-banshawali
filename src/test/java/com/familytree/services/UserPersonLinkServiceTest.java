package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersonLinkServiceTest {

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private PersonDisplayHelper personDisplay;

    @InjectMocks
    private UserPersonLinkService service;

    private UserAccount account(long id) {
        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Person person(long id) {
        Person person = new Person();
        person.setId(id);
        return person;
    }

    @Test
    void createVerifiedLinkSavesANewVerifiedLink() {
        UserAccount account = account(6L);
        Person person = person(416L);

        service.createVerifiedLink(account, person);

        ArgumentCaptor<UserPersonLink> captor = ArgumentCaptor.forClass(UserPersonLink.class);
        org.mockito.Mockito.verify(userPersonLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAccount()).isEqualTo(account);
        assertThat(captor.getValue().getPerson()).isEqualTo(person);
        assertThat(captor.getValue().getLinkStatus()).isEqualTo(UserPersonLinkStatus.VERIFIED);
        assertThat(captor.getValue().getVerifiedAt()).isNotNull();
    }

    @Test
    void createVerifiedLinkThrowsWhenAccountAlreadyHasVerifiedLink() {
        UserAccount account = account(6L);
        Person person = person(416L);

        UserPersonLink existing = new UserPersonLink();
        existing.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        when(userPersonLinkRepository.findByUserAccountId(6L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createVerifiedLink(account, person))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked to a person");
        org.mockito.Mockito.verify(userPersonLinkRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createVerifiedLinkThrowsWhenPersonAlreadyVerifiedLinkedToAnotherAccount() {
        UserAccount account = account(6L);
        Person person = person(416L);
        when(personDisplay.englishFullName(person)).thenReturn("Bhojraj Bhatta");

        UserPersonLink existing = new UserPersonLink();
        existing.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        when(userPersonLinkRepository.findByPersonId(416L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createVerifiedLink(account, person))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bhojraj Bhatta")
                .hasMessageContaining("already linked to another account");
        org.mockito.Mockito.verify(userPersonLinkRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createVerifiedLinkTranslatesAConcurrentDuplicateIntoAFriendlyError() {
        // Simulates the race the pre-checks above can't close on their own:
        // both pass (no existing VERIFIED link visible yet), then the save
        // itself hits uk_user_person_links_verified_account/_person (V24) --
        // the actual backstop.
        UserAccount account = account(6L);
        Person person = person(416L);
        when(userPersonLinkRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.createVerifiedLink(account, person))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("just linked by another request");
    }
}
