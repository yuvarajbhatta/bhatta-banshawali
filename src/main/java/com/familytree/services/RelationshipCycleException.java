package com.familytree.services;

/**
 * Thrown when saving a relationship would make a person their own ancestor
 * (directly or transitively through an existing parent/child chain).
 */
public class RelationshipCycleException extends RuntimeException {

    public RelationshipCycleException(String message) {
        super(message);
    }
}
