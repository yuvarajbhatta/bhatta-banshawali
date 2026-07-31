package com.familytree.services;

/**
 * Thrown by PasswordResetService/EmailVerificationService when a token
 * doesn't exist, was already consumed, or has expired -- see
 * TokenService#consumeToken.
 */
public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException(String message) {
        super(message);
    }
}
