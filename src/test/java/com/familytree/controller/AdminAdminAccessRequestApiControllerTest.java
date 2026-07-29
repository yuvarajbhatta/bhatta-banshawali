package com.familytree.controller;

import com.familytree.dto.AdminAccessRequestDto;
import com.familytree.services.AdminAccessRequestService;
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
class AdminAdminAccessRequestApiControllerTest {

    @Mock
    private AdminAccessRequestService adminAccessRequestService;

    @Mock
    private Authentication authentication;

    private AdminAdminAccessRequestApiController controller() {
        return new AdminAdminAccessRequestApiController(adminAccessRequestService);
    }

    @Test
    void listDelegatesToService() {
        AdminAccessRequestDto dto = new AdminAccessRequestDto(1L, 6L, "member@example.com", null, LocalDateTime.now());
        when(adminAccessRequestService.findPending()).thenReturn(List.of(dto));

        List<AdminAccessRequestDto> result = controller().list();

        assertThat(result).containsExactly(dto);
    }

    @Test
    void approveDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().approve(10L, authentication);

        verify(adminAccessRequestService).approve(10L, "admin@example.com");
    }

    @Test
    void denyDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().deny(10L, authentication);

        verify(adminAccessRequestService).deny(10L, "admin@example.com");
    }
}
