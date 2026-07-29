package com.familytree.controller;

import com.familytree.dto.AdminAccountSignupInfoUpdateDto;
import com.familytree.dto.AdminUserAccountDto;
import com.familytree.dto.LinkAccountRequestDto;
import com.familytree.entity.UserAccountStatus;
import com.familytree.services.UserAccountAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserAccountApiControllerTest {

    @Mock
    private UserAccountAdminService userAccountAdminService;

    @Mock
    private Authentication authentication;

    private AdminUserAccountApiController controller() {
        return new AdminUserAccountApiController(userAccountAdminService);
    }

    @Test
    void listDelegatesToService() {
        AdminUserAccountDto dto = new AdminUserAccountDto(
                1L, "yuva@example.com", UserAccountStatus.ACTIVE, "en", LocalDateTime.now(), null,
                null, null, null, null, null, null);
        when(userAccountAdminService.listAll()).thenReturn(List.of(dto));

        List<AdminUserAccountDto> result = controller().list();

        assertThat(result).containsExactly(dto);
    }

    @Test
    void linkDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        LinkAccountRequestDto request = new LinkAccountRequestDto();
        request.setPersonId(416L);

        controller().link(6L, request, authentication);

        verify(userAccountAdminService).link(6L, 416L, "admin@example.com");
    }

    @Test
    void unlinkDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().unlink(6L, authentication);

        verify(userAccountAdminService).unlink(6L, "admin@example.com");
    }

    @Test
    void updateSignupInfoDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        AdminAccountSignupInfoUpdateDto request = new AdminAccountSignupInfoUpdateDto();

        controller().updateSignupInfo(6L, request, authentication);

        verify(userAccountAdminService).updateSignupInfo(6L, request, "admin@example.com");
    }

    @Test
    void disableDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().disable(5L, authentication);

        verify(userAccountAdminService).disable(5L, "admin@example.com");
    }

    @Test
    void enableDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().enable(5L, authentication);

        verify(userAccountAdminService).enable(5L, "admin@example.com");
    }

    @Test
    void deleteDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().delete(5L, authentication);

        verify(userAccountAdminService).delete(5L, "admin@example.com");
    }
}
