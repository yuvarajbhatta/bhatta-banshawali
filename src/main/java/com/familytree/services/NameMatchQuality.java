package com.familytree.services;

/**
 * Result of NameMatcher.matchQuality() -- EXACT (identical or transliterated
 * match), FUZZY (same-script spelling variant within a small edit distance),
 * or NONE.
 */
public enum NameMatchQuality {
    EXACT,
    FUZZY,
    NONE
}
