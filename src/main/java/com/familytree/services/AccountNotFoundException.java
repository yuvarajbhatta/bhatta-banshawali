package com.familytree.services;

/**
 * Thrown by PasswordResetService when no UserAccount matches the
 * requested email. Deliberately surfaces as a clear error rather than a
 * generic anti-enumeration-safe response -- matching the precedent
 * EmailAlreadyRegisteredException already set for signup on this small,
 * invitation-oriented family site (see that class's javadoc).
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
