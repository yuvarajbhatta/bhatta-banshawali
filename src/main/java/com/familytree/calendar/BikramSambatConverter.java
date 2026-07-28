package com.familytree.calendar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Converts between the Gregorian (AD) and Bikram Sambat (BS) calendars.
 *
 * Bikram Sambat month lengths are not computable from a formula -- they
 * are set year by year (traditionally via astronomical calculation, then
 * published in the Nepali calendar/patro) -- so this uses a reference
 * table rather than approximate arithmetic, per docs/13 "AD/BS date
 * strategy".
 *
 * The table below (BS 2000-2090, i.e. roughly AD 1943-04-14 to
 * AD 2034-04-13) is ported from two independent published sources that
 * agree on every value checked:
 *   - "nepali-date-converter" by Subesh Bhandari (MIT license),
 *     https://github.com/subeshb1/Nepali-Date -- an actively maintained,
 *     widely used library with its own test suite and multiple major
 *     releases.
 *   - "nepali-calender" by Roshan Garu (Apache-2.0 license),
 *     io.github.rongaru:nepali-calender on Maven Central -- independently
 *     authored, same month-length values for every year checked.
 * Agreement between two independently maintained implementations is the
 * closest practical substitute for an authoritative government dataset;
 * neither library alone was judged reliable enough to depend on directly
 * (see docs/adr and the Phase 3 notes: the Maven Central one has no
 * BS-to-AD direction, no tests, and a slow day-by-day algorithm).
 *
 * Round-trip conversion (AD -> BS -> AD) is exhaustively tested across
 * every single day in the supported range -- see BikramSambatConverterTest.
 */
public final class BikramSambatConverter {

    public static final int MIN_SUPPORTED_BS_YEAR = 2000;
    public static final int MAX_SUPPORTED_BS_YEAR = 2090;

    /** BS 2000-01-01. */
    private static final LocalDate EPOCH_AD_DATE = LocalDate.of(1943, 4, 14);

    public static final LocalDate MIN_SUPPORTED_AD_DATE = EPOCH_AD_DATE;

    // BS 2000 .. 2090 inclusive, month lengths for Baisakh(1) .. Chaitra(12).
    private static final int[][] MONTH_LENGTHS = {
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2000
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2001
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2002
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2003
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2004
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2005
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2006
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2007
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 }, // BS 2008
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2009
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2010
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2011
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 }, // BS 2012
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2013
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2014
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2015
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 }, // BS 2016
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2017
            { 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2018
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2019
            { 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2020
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2021
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 }, // BS 2022
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2023
            { 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2024
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2025
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2026
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2027
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2028
            { 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30 }, // BS 2029
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2030
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2031
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2032
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2033
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2034
            { 30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 }, // BS 2035
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2036
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2037
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2038
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 }, // BS 2039
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2040
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2041
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2042
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 }, // BS 2043
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2044
            { 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2045
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2046
            { 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2047
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2048
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 }, // BS 2049
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2050
            { 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2051
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2052
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 }, // BS 2053
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2054
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2055
            { 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30 }, // BS 2056
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2057
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2058
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2059
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2060
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2061
            { 30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31 }, // BS 2062
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2063
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2064
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2065
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 }, // BS 2066
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2067
            { 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2068
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2069
            { 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 }, // BS 2070
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2071
            { 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 }, // BS 2072
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2073
            { 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2074
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2075
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 }, // BS 2076
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2077
            { 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2078
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2079
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 }, // BS 2080
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2081
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2082
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2083
            { 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 }, // BS 2084
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 }, // BS 2085
            { 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 }, // BS 2086
            { 31, 31, 32, 31, 31, 31, 30, 30, 29, 30, 30, 30 }, // BS 2087
            { 30, 31, 32, 32, 30, 31, 30, 30, 29, 30, 30, 30 }, // BS 2088
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30 }, // BS 2089
            { 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30 }, // BS 2090
    };

    /** Cumulative days from the epoch to the start of BS year (2000 + index). */
    private static final int[] DAYS_BEFORE_YEAR = new int[MONTH_LENGTHS.length + 1];

    /** Cumulative days within each year to the start of each month. */
    private static final int[][] DAYS_BEFORE_MONTH_IN_YEAR = new int[MONTH_LENGTHS.length][13];

    static {
        int runningTotal = 0;
        for (int yearIndex = 0; yearIndex < MONTH_LENGTHS.length; yearIndex++) {
            DAYS_BEFORE_YEAR[yearIndex] = runningTotal;

            int daysBeforeMonth = 0;
            for (int month = 0; month < 12; month++) {
                DAYS_BEFORE_MONTH_IN_YEAR[yearIndex][month] = daysBeforeMonth;
                daysBeforeMonth += MONTH_LENGTHS[yearIndex][month];
            }
            DAYS_BEFORE_MONTH_IN_YEAR[yearIndex][12] = daysBeforeMonth;

            runningTotal += daysBeforeMonth;
        }
        DAYS_BEFORE_YEAR[MONTH_LENGTHS.length] = runningTotal;
    }

    private static final long TOTAL_SUPPORTED_DAYS = DAYS_BEFORE_YEAR[MONTH_LENGTHS.length];

    public static final LocalDate MAX_SUPPORTED_AD_DATE = EPOCH_AD_DATE.plusDays(TOTAL_SUPPORTED_DAYS - 1);

    private BikramSambatConverter() {
    }

    public static boolean isSupported(LocalDate adDate) {
        return !adDate.isBefore(MIN_SUPPORTED_AD_DATE) && !adDate.isAfter(MAX_SUPPORTED_AD_DATE);
    }

    public static boolean isSupported(BikramSambatDate bsDate) {
        int yearIndex = bsDate.year() - MIN_SUPPORTED_BS_YEAR;
        if (yearIndex < 0 || yearIndex >= MONTH_LENGTHS.length) {
            return false;
        }
        return bsDate.day() <= MONTH_LENGTHS[yearIndex][bsDate.month() - 1];
    }

    public static LocalDate toAd(BikramSambatDate bsDate) {
        int yearIndex = bsDate.year() - MIN_SUPPORTED_BS_YEAR;
        if (yearIndex < 0 || yearIndex >= MONTH_LENGTHS.length) {
            throw new UnsupportedDateRangeException(
                    "BS year " + bsDate.year() + " is outside the supported range "
                            + MIN_SUPPORTED_BS_YEAR + "-" + MAX_SUPPORTED_BS_YEAR);
        }

        int monthLength = MONTH_LENGTHS[yearIndex][bsDate.month() - 1];
        if (bsDate.day() > monthLength) {
            throw new IllegalArgumentException(
                    "BS " + bsDate.year() + "-" + bsDate.month() + " only has " + monthLength + " days, got day "
                            + bsDate.day());
        }

        long daysSinceEpoch = DAYS_BEFORE_YEAR[yearIndex]
                + DAYS_BEFORE_MONTH_IN_YEAR[yearIndex][bsDate.month() - 1]
                + (bsDate.day() - 1);

        return EPOCH_AD_DATE.plusDays(daysSinceEpoch);
    }

    public static BikramSambatDate toBs(LocalDate adDate) {
        long daysSinceEpoch = ChronoUnit.DAYS.between(EPOCH_AD_DATE, adDate);
        if (daysSinceEpoch < 0 || daysSinceEpoch >= TOTAL_SUPPORTED_DAYS) {
            throw new UnsupportedDateRangeException(
                    "Date " + adDate + " is outside the supported range " + MIN_SUPPORTED_AD_DATE + " to "
                            + MAX_SUPPORTED_AD_DATE);
        }

        int yearIndex = 0;
        while (yearIndex + 1 < DAYS_BEFORE_YEAR.length && DAYS_BEFORE_YEAR[yearIndex + 1] <= daysSinceEpoch) {
            yearIndex++;
        }

        long dayOfYear = daysSinceEpoch - DAYS_BEFORE_YEAR[yearIndex];

        int month = 0;
        while (month + 1 < 12 && DAYS_BEFORE_MONTH_IN_YEAR[yearIndex][month + 1] <= dayOfYear) {
            month++;
        }

        int day = (int) (dayOfYear - DAYS_BEFORE_MONTH_IN_YEAR[yearIndex][month] + 1);

        return new BikramSambatDate(yearIndex + MIN_SUPPORTED_BS_YEAR, month + 1, day);
    }
}
