package com.familytree.calendar;

/**
 * A calendar date in the Bikram Sambat (BS) calendar. Month is 1-indexed
 * (1 = Baisakh ... 12 = Chaitra), matching how the rest of the codebase
 * (and users) refer to months, unlike the 0-indexed convention some
 * JavaScript BS libraries use internally.
 */
public record BikramSambatDate(int year, int month, int day) {

    public BikramSambatDate {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12, was " + month);
        }
        if (day < 1 || day > 32) {
            throw new IllegalArgumentException("Day must be between 1 and 32, was " + day);
        }
    }
}
