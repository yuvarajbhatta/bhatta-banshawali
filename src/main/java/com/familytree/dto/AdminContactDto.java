package com.familytree.dto;

/**
 * One admin's contact identity, shown to members on the Help & Contact
 * page so they know who to reach out to (see AdminContactService).
 * email is null for a legacy AppUser admin login, which stores only a
 * username -- nothing else to show for that account.
 */
public record AdminContactDto(String displayName, String email) {
}
