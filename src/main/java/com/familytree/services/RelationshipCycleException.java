package com.familytree.services;

/**
 * Thrown when saving a relationship would create an impossible family link:
 * a person their own ancestor (directly or transitively through an
 * existing parent/child chain), a spouse who's also an ancestor/descendant,
 * or the same person recorded as both FATHER and MOTHER of the same
 * child (including a gender mismatch against an existing Person.gender).
 */
public class RelationshipCycleException extends RuntimeException {

    public RelationshipCycleException(String message) {
        super(message);
    }
}
