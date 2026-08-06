package com.familytree.dto;

/**
 * status is always the same shape and value ("PENDING_REVIEW")
 * regardless of match confidence, whether the email was already
 * registered, or anything else -- the frontend localizes this into
 * copy, but the wire value itself must never vary in a way that lets a
 * caller distinguish outcomes (docs/05 "Prevent account enumeration and
 * family-member enumeration"). photoUploadToken doesn't carry any such
 * signal (it's an opaque random handle, not derived from match
 * outcome), so it's exempt from that constraint -- see
 * SignupService.uploadPendingPhoto.
 */
public record SignupResponseDto(String status, String photoUploadToken) {

    public static SignupResponseDto pendingReview(String photoUploadToken) {
        return new SignupResponseDto("PENDING_REVIEW", photoUploadToken);
    }
}
