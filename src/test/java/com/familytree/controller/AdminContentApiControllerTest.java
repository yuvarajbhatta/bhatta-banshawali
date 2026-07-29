package com.familytree.controller;

import com.familytree.dto.AdminArticleDto;
import com.familytree.dto.AdminArticleRequestDto;
import com.familytree.entity.ArticleStatus;
import com.familytree.services.ContentAdminService;
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
class AdminContentApiControllerTest {

    @Mock
    private ContentAdminService contentAdminService;

    @Mock
    private Authentication authentication;

    private AdminContentApiController controller() {
        return new AdminContentApiController(contentAdminService);
    }

    private AdminArticleDto dto() {
        return new AdminArticleDto(1L, "about-banshawali", "Title", null, "Body", null,
                ArticleStatus.DRAFT, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listDelegatesToService() {
        when(contentAdminService.listAll()).thenReturn(List.of(dto()));

        List<AdminArticleDto> result = controller().list();

        assertThat(result).hasSize(1);
    }

    @Test
    void getDelegatesToService() {
        when(contentAdminService.getById(1L)).thenReturn(dto());

        AdminArticleDto result = controller().get(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void createDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        AdminArticleRequestDto request = new AdminArticleRequestDto();
        when(contentAdminService.create(request, "admin@example.com")).thenReturn(dto());

        controller().create(request, authentication);

        verify(contentAdminService).create(request, "admin@example.com");
    }

    @Test
    void updateDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        AdminArticleRequestDto request = new AdminArticleRequestDto();
        when(contentAdminService.update(1L, request, "admin@example.com")).thenReturn(dto());

        controller().update(1L, request, authentication);

        verify(contentAdminService).update(1L, request, "admin@example.com");
    }

    @Test
    void submitForReviewDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(contentAdminService.submitForReview(1L, "admin@example.com")).thenReturn(dto());

        controller().submitForReview(1L, authentication);

        verify(contentAdminService).submitForReview(1L, "admin@example.com");
    }

    @Test
    void publishDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(contentAdminService.publish(1L, "admin@example.com")).thenReturn(dto());

        controller().publish(1L, authentication);

        verify(contentAdminService).publish(1L, "admin@example.com");
    }

    @Test
    void unpublishDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(contentAdminService.unpublish(1L, "admin@example.com")).thenReturn(dto());

        controller().unpublish(1L, authentication);

        verify(contentAdminService).unpublish(1L, "admin@example.com");
    }

    @Test
    void revertToDraftDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(contentAdminService.revertToDraft(1L, "admin@example.com")).thenReturn(dto());

        controller().revertToDraft(1L, authentication);

        verify(contentAdminService).revertToDraft(1L, "admin@example.com");
    }

    @Test
    void deleteDelegatesToServiceWithActorName() {
        when(authentication.getName()).thenReturn("admin@example.com");

        controller().delete(1L, authentication);

        verify(contentAdminService).delete(1L, "admin@example.com");
    }
}
