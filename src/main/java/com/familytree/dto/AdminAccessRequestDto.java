package com.familytree.dto;

import java.time.LocalDateTime;

/** One row in the admin's Admin Access Requests review queue. */
public record AdminAccessRequestDto(
        Long id,
        Long userAccountId,
        String email,
        String linkedPersonName,
        LocalDateTime requestedAt
) {
}
