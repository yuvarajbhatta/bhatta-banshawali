package com.familytree.services;

import com.familytree.dto.AdminArticleDto;
import com.familytree.dto.AdminArticleRequestDto;
import com.familytree.entity.ArticleStatus;
import com.familytree.entity.HistoricalArticle;
import com.familytree.repository.HistoricalArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentAdminServiceTest {

    @Mock
    private HistoricalArticleRepository historicalArticleRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ContentAdminService service;

    private HistoricalArticle article(long id, String slug, ArticleStatus status) {
        HistoricalArticle article = new HistoricalArticle();
        ReflectionTestUtils.setField(article, "id", id);
        article.setSlug(slug);
        article.setTitleEn("Title");
        article.setBodyEn("Body");
        article.setStatus(status);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        return article;
    }

    private AdminArticleRequestDto request(String slug) {
        AdminArticleRequestDto request = new AdminArticleRequestDto();
        request.setSlug(slug);
        request.setTitleEn("New Title");
        request.setBodyEn("New body text.");
        return request;
    }

    @Test
    void createSavesAsDraftAndLogsIt() {
        when(historicalArticleRepository.existsBySlug("family-history")).thenReturn(false);

        AdminArticleDto result = service.create(request("family-history"), "admin");

        ArgumentCaptor<HistoricalArticle> captor = ArgumentCaptor.forClass(HistoricalArticle.class);
        verify(historicalArticleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(captor.getValue().getSlug()).isEqualTo("family-history");
        assertThat(result.status()).isEqualTo(ArticleStatus.DRAFT);
        verify(auditLogService).record(AuditLogService.ACTION_CONTENT_CREATED, AuditLogService.ENTITY_CONTENT,
                captor.getValue().getId(), "Created content \"family-history\"", "admin");
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(historicalArticleRepository.existsBySlug("about-banshawali")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("about-banshawali"), "admin"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(historicalArticleRepository, never()).save(any());
    }

    @Test
    void updateEditsContentAndBlankNepaliBecomesNull() {
        HistoricalArticle existing = article(1L, "about-banshawali", ArticleStatus.DRAFT);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(historicalArticleRepository.existsBySlugAndIdNot("about-banshawali", 1L)).thenReturn(false);

        AdminArticleRequestDto request = request("about-banshawali");
        request.setTitleNe("  ");
        request.setBodyNe("");

        service.update(1L, request, "admin");

        assertThat(existing.getTitleEn()).isEqualTo("New Title");
        assertThat(existing.getTitleNe()).isNull();
        assertThat(existing.getBodyNe()).isNull();
        verify(auditLogService).record(AuditLogService.ACTION_CONTENT_UPDATED, AuditLogService.ENTITY_CONTENT,
                1L, "Edited content \"about-banshawali\"", "admin");
    }

    @Test
    void updateRejectsSlugAlreadyUsedByAnotherArticle() {
        HistoricalArticle existing = article(1L, "about-banshawali", ArticleStatus.DRAFT);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(historicalArticleRepository.existsBySlugAndIdNot("membership-verification", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("membership-verification"), "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitForReviewMovesDraftToInReview() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.DRAFT);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.submitForReview(1L, "admin");

        assertThat(existing.getStatus()).isEqualTo(ArticleStatus.IN_REVIEW);
    }

    @Test
    void submitForReviewThrowsWhenNotDraft() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.PUBLISHED);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.submitForReview(1L, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publishSetsPublishedAtAndStatus() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.IN_REVIEW);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.publish(1L, "admin");

        assertThat(existing.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(existing.getPublishedAt()).isNotNull();
    }

    @Test
    void publishThrowsWhenAlreadyPublished() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.PUBLISHED);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.publish(1L, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unpublishMovesPublishedToUnpublished() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.PUBLISHED);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.unpublish(1L, "admin");

        assertThat(existing.getStatus()).isEqualTo(ArticleStatus.UNPUBLISHED);
    }

    @Test
    void revertToDraftThrowsWhenAlreadyDraft() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.DRAFT);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.revertToDraft(1L, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revertToDraftMovesUnpublishedToDraft() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.UNPUBLISHED);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.revertToDraft(1L, "admin");

        assertThat(existing.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void deleteRemovesTheArticleAndLogsIt() {
        HistoricalArticle existing = article(1L, "family-history", ArticleStatus.DRAFT);
        when(historicalArticleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L, "admin");

        verify(historicalArticleRepository).delete(existing);
        verify(auditLogService).record(AuditLogService.ACTION_CONTENT_DELETED, AuditLogService.ENTITY_CONTENT,
                1L, "Deleted content \"family-history\"", "admin");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(historicalArticleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listAllOrdersByUpdatedAtDescending() {
        HistoricalArticle older = article(1L, "older", ArticleStatus.DRAFT);
        older.setUpdatedAt(LocalDateTime.now().minusDays(2));
        HistoricalArticle newer = article(2L, "newer", ArticleStatus.DRAFT);
        newer.setUpdatedAt(LocalDateTime.now());
        when(historicalArticleRepository.findAll()).thenReturn(List.of(older, newer));

        List<AdminArticleDto> result = service.listAll();

        assertThat(result).extracting(AdminArticleDto::slug).containsExactly("newer", "older");
    }
}
