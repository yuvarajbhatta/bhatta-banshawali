package com.familytree.services;

import com.familytree.entity.Person;

import java.util.Arrays;

/**
 * Splits a plain "First Middle Last" string into Person name fields -- the
 * one shared implementation for both UserAccountAdminService's "apply
 * submitted signup info to an already-linked Person" flow and
 * VerificationReviewService's "create a brand-new Person from signup info"
 * flow, so there is exactly one full-name-string parser.
 */
public final class FullNameParser {

    private FullNameParser() {
    }

    public static void applyTo(Person person, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return;
        }
        String[] parts = fullName.trim().split("\\s+");
        person.setFirstName(parts[0]);
        if (parts.length == 1) {
            person.setMiddleName(null);
            return;
        }
        person.setLastName(parts[parts.length - 1]);
        person.setMiddleName(parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)) : null);
    }
}
