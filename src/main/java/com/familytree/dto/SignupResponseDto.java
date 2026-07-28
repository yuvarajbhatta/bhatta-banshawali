package com.familytree.dto;

/**
 * Always the same shape and value ("PENDING_REVIEW") regardless of match
 * confidence, whether the email was already registered, or anything else
 * -- the frontend localizes this into copy, but the wire value itself
 * must never vary in a way that lets a caller distinguish outcomes
 * (docs/05 "Prevent account enumeration and family-member enumeration").
 */
public record SignupResponseDto(String status) {

    public static SignupResponseDto pendingReview() {
        return new SignupResponseDto("PENDING_REVIEW");
    }
}
