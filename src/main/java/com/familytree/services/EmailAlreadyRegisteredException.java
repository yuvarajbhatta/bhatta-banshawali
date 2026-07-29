package com.familytree.services;

/**
 * Thrown by SignupService when a signup's email already has an account.
 * A deliberate reversal of the original silent-no-op/anti-enumeration
 * design (docs/05-auth-and-verification.md, docs/09-security-threat-model.md
 * previously documented constant-response-shape signup specifically to
 * prevent an attacker from discovering which emails are registered) --
 * the site owner decided the UX cost of a vague error outweighs
 * enumeration risk for a small, invitation-oriented family site. See
 * docs/05's "Anti-Enumeration Guarantees" section.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
