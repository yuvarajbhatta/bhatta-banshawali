package com.familytree.dto;

import java.time.LocalDateTime;

public record ArticleSummaryDto(
        String slug,
        String titleEn,
        String titleNe,
        LocalDateTime publishedAt
) {
}
