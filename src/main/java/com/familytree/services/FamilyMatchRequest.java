package com.familytree.services;

import java.time.LocalDate;

/**
 * What a signup applicant submitted, for the family-match engine to
 * evaluate against existing Person records.
 */
public record FamilyMatchRequest(
        String applicantFullName,
        String fatherName,
        String grandfatherName,
        LocalDate dobAd
) {
}
