package com.familytree.services;

/**
 * Thrown by PasswordResetService when a link token doesn't exist, was
 * already consumed, or has expired (see TokenService#consumeToken), and
 * reused by EmailVerificationService/AdminAccessRequestService for the
 * equivalent OTP failures (see OtpService#verify) since both are the same
 * "invalid or expired, try again" shape from the client's perspective.
 */
public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException(String message) {
        super(message);
    }
}
