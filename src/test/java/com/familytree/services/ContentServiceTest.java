package com.familytree.services;

import com.familytree.dto.ArticleDto;
import com.familytree.entity.ArticleStatus;
import com.familytree.entity.HistoricalArticle;
import com.familytree.repository.HistoricalArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private HistoricalArticleRepository historicalArticleRepository;

    @InjectMocks
    private ContentService contentService;

    @Test
    void getPublishedArticleMapsToDto() {
        HistoricalArticle article = new HistoricalArticle();
        article.setSlug("about-banshawali");
        article.setTitleEn("About the Banshawali");
        article.setTitleNe("बंशावलीको बारेमा");
        article.setBodyEn("English body");
        article.setBodyNe("Nepali body");
        article.setPublishedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(historicalArticleRepository.findBySlugAndStatus("about-banshawali", ArticleStatus.PUBLISHED))
                .thenReturn(Optional.of(article));

        Optional<ArticleDto> result = contentService.getPublishedArticle("about-banshawali");

        assertThat(result).isPresent();
        assertThat(result.get().titleEn()).isEqualTo("About the Banshawali");
        assertThat(result.get().bodyNe()).isEqualTo("Nepali body");
    }

    @Test
    void getPublishedArticleReturnsEmptyWhenNotPublished() {
        when(historicalArticleRepository.findBySlugAndStatus("draft-only", ArticleStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThat(contentService.getPublishedArticle("draft-only")).isEmpty();
    }

    @Test
    void listPublishedArticlesMapsEachToSummaryDto() {
        HistoricalArticle article = new HistoricalArticle();
        article.setSlug("membership-verification");
        article.setTitleEn("How Membership Verification Works");
        when(historicalArticleRepository.findAllByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED))
                .thenReturn(List.of(article));

        assertThat(contentService.listPublishedArticles())
                .extracting(summary -> summary.slug())
                .containsExactly("membership-verification");
    }
}
