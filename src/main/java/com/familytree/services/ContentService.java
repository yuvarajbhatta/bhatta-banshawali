package com.familytree.services;

import com.familytree.dto.ArticleDto;
import com.familytree.dto.ArticleSummaryDto;
import com.familytree.entity.ArticleStatus;
import com.familytree.entity.HistoricalArticle;
import com.familytree.repository.HistoricalArticleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    private final HistoricalArticleRepository historicalArticleRepository;

    public ContentService(HistoricalArticleRepository historicalArticleRepository) {
        this.historicalArticleRepository = historicalArticleRepository;
    }

    public Optional<ArticleDto> getPublishedArticle(String slug) {
        return historicalArticleRepository.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED)
                .map(ContentService::toDto);
    }

    public List<ArticleSummaryDto> listPublishedArticles() {
        return historicalArticleRepository.findAllByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED).stream()
                .map(ContentService::toSummaryDto)
                .toList();
    }

    private static ArticleDto toDto(HistoricalArticle article) {
        return new ArticleDto(
                article.getSlug(),
                article.getTitleEn(),
                article.getTitleNe(),
                article.getBodyEn(),
                article.getBodyNe(),
                article.getPublishedAt()
        );
    }

    private static ArticleSummaryDto toSummaryDto(HistoricalArticle article) {
        return new ArticleSummaryDto(
                article.getSlug(),
                article.getTitleEn(),
                article.getTitleNe(),
                article.getPublishedAt()
        );
    }
}
