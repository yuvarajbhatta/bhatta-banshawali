package com.familytree.dto;

import java.time.LocalDateTime;

/**
 * One photo on an announcement post. The image bytes are fetched
 * separately via GET .../{postId}/photos/{photoId}/file, same pattern
 * as PersonPhotoDto.
 */
public record AnnouncementPhotoDto(
        Long id,
        String caption,
        LocalDateTime uploadedAt
) {
}
