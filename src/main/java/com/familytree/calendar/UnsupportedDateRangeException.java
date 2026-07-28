package com.familytree.calendar;

/**
 * Thrown when a date falls outside the range the Bikram Sambat conversion
 * table covers (BS 2000-01-01 to BS 2090-12-30, roughly AD 1943-04-14 to
 * AD 2034-04-13). This is a real limitation of the available reference
 * calendar data, not a bug -- callers must treat conversion as best-effort
 * for dates outside this range (e.g. older ancestors' birth dates) rather
 * than blocking the record from being saved at all. See
 * docs/13 "Date Representation" and docs/04-data-model.md.
 */
public class UnsupportedDateRangeException extends RuntimeException {

    public UnsupportedDateRangeException(String message) {
        super(message);
    }
}
