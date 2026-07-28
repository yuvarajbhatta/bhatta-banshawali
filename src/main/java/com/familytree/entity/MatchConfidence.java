package com.familytree.entity;

/**
 * How confidently a signup applicant's stated name/lineage matches an
 * existing family branch -- see docs/05-auth-and-verification.md.
 */
public enum MatchConfidence {
    HIGH,
    MEDIUM,
    LOW
}
