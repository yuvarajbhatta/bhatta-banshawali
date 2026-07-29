package com.familytree.dto;

import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.CorrectionRequestStatus;

import java.time.LocalDateTime;

/**
 * One row in the admin correction review queue, mirrors
 * admin-corrections.html. personId/personName is a plain summary
 * (not the full PersonSummaryDto redaction dance) since the admin
 * viewing this queue always has full visibility by definition.
 */
public record AdminCorrectionSummaryDto(
        Long id,
        Long personId,
        String personName,
        CorrectablePersonField field,
        String currentValueSnapshot,
        String proposedValue,
        String reason,
        String submittedByEmail,
        LocalDateTime submittedAt,
        CorrectionRequestStatus status,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String decisionNote
) {
}
