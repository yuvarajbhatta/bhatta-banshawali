package com.familytree.controller;

import com.familytree.dto.AdminAccessRequestConfirmDto;
import com.familytree.dto.MyAdminAccessRequestStatusDto;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.AdminAccessRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccessRequestControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AdminAccessRequestService adminAccessRequestService;

    @Mock
    private Authentication authentication;

    private AdminAccessRequestController controller() {
        return new AdminAccessRequestController(userAccountRepository, adminAccessRequestService);
    }

    private UserAccount account(long id, String email) {
        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", id);
        account.setEmail(email);
        return account;
    }

    @Test
    void statusDelegatesToServiceForTheCurrentAccount() {
        when(authentication.getName()).thenReturn("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account(6L, "member@example.com")));
        when(adminAccessRequestService.myStatus(6L)).thenReturn(MyAdminAccessRequestStatusDto.none());

        MyAdminAccessRequestStatusDto result = controller().status(authentication);

        assertThat(result).isEqualTo(MyAdminAccessRequestStatusDto.none());
    }

    @Test
    void requestDelegatesToServiceForTheCurrentAccount() {
        when(authentication.getName()).thenReturn("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account(6L, "member@example.com")));

        controller().request(authentication);

        verify(adminAccessRequestService).request(6L);
    }

    @Test
    void confirmDelegatesToServiceForTheCurrentAccount() {
        when(authentication.getName()).thenReturn("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account(6L, "member@example.com")));

        controller().confirm(authentication, new AdminAccessRequestConfirmDto("123456"));

        verify(adminAccessRequestService).confirmRequest(6L, "123456");
    }

    @Test
    void statusThrowsNotFoundForALegacyAppUserLogin() {
        when(authentication.getName()).thenReturn("admin");
        when(userAccountRepository.findByEmail("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller().status(authentication)).isInstanceOf(ResponseStatusException.class);
    }
}
