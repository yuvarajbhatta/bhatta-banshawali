package com.familytree.dto;

/**
 * What the current member sees for their own account:
 * "NONE" (never requested, or their last request was denied and they
 * haven't asked again), "AWAITING_OTP" (requested, needs to confirm the
 * emailed code before an admin can even see it), "PENDING" (OTP
 * confirmed, awaiting review), or "ALREADY_ADMIN" (nothing to request).
 * Drives whether the sidebar's "Request Admin Access" button is shown,
 * disabled, replaced with an inline code-entry step, or replaced with a
 * pending note.
 */
public record MyAdminAccessRequestStatusDto(String status) {

    public static MyAdminAccessRequestStatusDto none() {
        return new MyAdminAccessRequestStatusDto("NONE");
    }

    public static MyAdminAccessRequestStatusDto awaitingOtp() {
        return new MyAdminAccessRequestStatusDto("AWAITING_OTP");
    }

    public static MyAdminAccessRequestStatusDto pending() {
        return new MyAdminAccessRequestStatusDto("PENDING");
    }

    public static MyAdminAccessRequestStatusDto alreadyAdmin() {
        return new MyAdminAccessRequestStatusDto("ALREADY_ADMIN");
    }
}
