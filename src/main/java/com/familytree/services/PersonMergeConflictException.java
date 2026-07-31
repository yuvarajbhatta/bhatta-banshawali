package com.familytree.services;

/**
 * Thrown when two Person records can't be merged as requested -- they're
 * already directly related, or both are linked to a verified user account.
 * Mirrors RelationshipCycleException; the controller maps this to 409.
 */
public class PersonMergeConflictException extends RuntimeException {

    public PersonMergeConflictException(String message) {
        super(message);
    }
}
