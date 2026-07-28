package com.familytree.dto;

import java.time.LocalDateTime;

/**
 * Full public-facing content for a single published article. Never the
 * entity itself -- {@code bodyNe}/{@code titleNe} are null when no Nepali
 * translation exists yet, so the frontend can fall back to English.
 */
public record ArticleDto(
        String slug,
        String titleEn,
        String titleNe,
        String bodyEn,
        String bodyNe,
        LocalDateTime publishedAt
) {
}
