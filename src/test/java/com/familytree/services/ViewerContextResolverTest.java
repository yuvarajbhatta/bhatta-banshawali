package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewerContextResolverTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    private ViewerContextResolver resolver() {
        return new ViewerContextResolver(userAccountRepository, userPersonLinkRepository);
    }

    @Test
    void recognizesAdminRole() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userAccountRepository.findByEmail("admin")).thenReturn(Optional.empty());

        ViewerContext viewer = resolver().resolve(authentication);

        assertThat(viewer.isAdmin()).isTrue();
    }

    @Test
    void nonAdminWithNoUserAccountHasNoLinkedPerson() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "regularadmin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userAccountRepository.findByEmail("regularadmin")).thenReturn(Optional.empty());

        ViewerContext viewer = resolver().resolve(authentication);

        assertThat(viewer.viewerPersonId()).isNull();
    }

    @Test
    void resolvesVerifiedLinkedPersonForAUserAccount() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "member@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", 10L);
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));

        Person person = new Person();
        person.setId(55L);
        UserPersonLink link = new UserPersonLink();
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        link.setPerson(person);
        when(userPersonLinkRepository.findByUserAccountId(10L)).thenReturn(List.of(link));

        ViewerContext viewer = resolver().resolve(authentication);

        assertThat(viewer.isAdmin()).isFalse();
        assertThat(viewer.viewerPersonId()).isEqualTo(55L);
    }

    @Test
    void ignoresNonVerifiedLinks() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "pending@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", 11L);
        when(userAccountRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(account));

        UserPersonLink link = new UserPersonLink();
        link.setLinkStatus(UserPersonLinkStatus.PENDING);
        link.setPerson(new Person());
        when(userPersonLinkRepository.findByUserAccountId(11L)).thenReturn(List.of(link));

        ViewerContext viewer = resolver().resolve(authentication);

        assertThat(viewer.viewerPersonId()).isNull();
    }
}
