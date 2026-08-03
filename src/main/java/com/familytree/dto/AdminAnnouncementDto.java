package com.familytree.dto;

import com.familytree.entity.AnnouncementCategory;
import com.familytree.entity.AnnouncementStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Full admin view of an AnnouncementPost, regardless of status. */
public record AdminAnnouncementDto(
        Long id,
        AnnouncementCategory category,
        String titleEn,
        String titleNe,
        String bodyEn,
        String bodyNe,
        AnnouncementStatus status,
        boolean pinned,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AnnouncementPhotoDto> photos
) {
}
