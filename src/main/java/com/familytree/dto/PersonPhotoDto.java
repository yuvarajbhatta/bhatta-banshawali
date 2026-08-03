package com.familytree.dto;

import java.time.LocalDateTime;

/**
 * One photo in a person's picture album (docs/06-ui-ux-specification.md
 * dashboard). Deliberately doesn't expose who uploaded it -- canDelete
 * already answers the only thing the viewer needs to know about
 * ownership, without leaking another member's account identity into
 * every viewer's response. The actual image bytes are fetched
 * separately via GET .../{id}/file.
 */
public record PersonPhotoDto(
        Long id,
        String caption,
        LocalDateTime uploadedAt,
        boolean canDelete
) {
}
