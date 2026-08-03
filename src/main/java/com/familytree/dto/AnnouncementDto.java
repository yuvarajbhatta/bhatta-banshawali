package com.familytree.dto;

import com.familytree.entity.AnnouncementCategory;

import java.time.LocalDateTime;
import java.util.List;

/** Member-facing view of a published announcement -- see AnnouncementService. */
public record AnnouncementDto(
        Long id,
        AnnouncementCategory category,
        String titleEn,
        String titleNe,
        String bodyEn,
        String bodyNe,
        boolean pinned,
        LocalDateTime publishedAt,
        List<AnnouncementPhotoDto> photos
) {
}
