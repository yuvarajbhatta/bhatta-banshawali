package com.familytree.dto;

import com.familytree.entity.ArticleStatus;

import java.time.LocalDateTime;

/** Full admin view of a HistoricalArticle, regardless of status. */
public record AdminArticleDto(
        Long id,
        String slug,
        String titleEn,
        String titleNe,
        String bodyEn,
        String bodyNe,
        ArticleStatus status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
