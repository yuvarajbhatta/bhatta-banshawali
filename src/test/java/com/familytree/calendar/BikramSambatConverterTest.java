package com.familytree.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BikramSambatConverterTest {

    @Test
    void epochDateConvertsBothWays() {
        assertThat(BikramSambatConverter.toAd(new BikramSambatDate(2000, 1, 1)))
                .isEqualTo(LocalDate.of(1943, 4, 14));
        assertThat(BikramSambatConverter.toBs(LocalDate.of(1943, 4, 14)))
                .isEqualTo(new BikramSambatDate(2000, 1, 1));
    }

    @Test
    void lastSupportedDateConvertsBothWays() {
        BikramSambatDate lastBsDate = new BikramSambatDate(2090, 12, 30);
        assertThat(BikramSambatConverter.toAd(lastBsDate)).isEqualTo(BikramSambatConverter.MAX_SUPPORTED_AD_DATE);
        assertThat(BikramSambatConverter.toBs(BikramSambatConverter.MAX_SUPPORTED_AD_DATE)).isEqualTo(lastBsDate);
    }

    /**
     * Nepali New Year (1 Baisakh) dates are publicly reported each year by
     * Nepali media/calendars -- these are independent, real-world facts,
     * not derived from the table under test, so they're a genuine external
     * check rather than a tautology.
     */
    @Test
    void nepaliNewYearDatesMatchPubliclyReportedAdDates() {
        assertThat(BikramSambatConverter.toAd(new BikramSambatDate(2080, 1, 1))).isEqualTo(LocalDate.of(2023, 4, 14));
        assertThat(BikramSambatConverter.toAd(new BikramSambatDate(2081, 1, 1))).isEqualTo(LocalDate.of(2024, 4, 13));
        assertThat(BikramSambatConverter.toAd(new BikramSambatDate(2082, 1, 1))).isEqualTo(LocalDate.of(2025, 4, 14));
    }

    @Test
    void convertsAtAMonthBoundary() {
        // BS 2080 Baisakh has 31 days (index 0 in its row); the day after
        // Baisakh 31 must roll over to Jestha 1, not stay in Baisakh.
        BikramSambatDate lastDayOfBaisakh = new BikramSambatDate(2080, 1, 31);
        LocalDate ad = BikramSambatConverter.toAd(lastDayOfBaisakh);

        BikramSambatDate nextDayInBs = BikramSambatConverter.toBs(ad.plusDays(1));

        assertThat(nextDayInBs).isEqualTo(new BikramSambatDate(2080, 2, 1));
    }

    @Test
    void convertsAtAYearBoundary() {
        BikramSambatDate lastDayOfYear = new BikramSambatDate(2080, 12,
                monthLengthFor(2080, 12));
        LocalDate ad = BikramSambatConverter.toAd(lastDayOfYear);

        BikramSambatDate nextDayInBs = BikramSambatConverter.toBs(ad.plusDays(1));

        assertThat(nextDayInBs).isEqualTo(new BikramSambatDate(2081, 1, 1));
    }

    @Test
    void rejectsAdDatesBeforeTheSupportedRange() {
        LocalDate tooEarly = BikramSambatConverter.MIN_SUPPORTED_AD_DATE.minusDays(1);
        assertThatThrownBy(() -> BikramSambatConverter.toBs(tooEarly))
                .isInstanceOf(UnsupportedDateRangeException.class);
        assertThat(BikramSambatConverter.isSupported(tooEarly)).isFalse();
    }

    @Test
    void rejectsAdDatesAfterTheSupportedRange() {
        LocalDate tooLate = BikramSambatConverter.MAX_SUPPORTED_AD_DATE.plusDays(1);
        assertThatThrownBy(() -> BikramSambatConverter.toBs(tooLate))
                .isInstanceOf(UnsupportedDateRangeException.class);
        assertThat(BikramSambatConverter.isSupported(tooLate)).isFalse();
    }

    @Test
    void rejectsBsYearsOutsideTheSupportedRange() {
        assertThatThrownBy(() -> BikramSambatConverter.toAd(new BikramSambatDate(1999, 1, 1)))
                .isInstanceOf(UnsupportedDateRangeException.class);
        assertThatThrownBy(() -> BikramSambatConverter.toAd(new BikramSambatDate(2091, 1, 1)))
                .isInstanceOf(UnsupportedDateRangeException.class);
        assertThat(BikramSambatConverter.isSupported(new BikramSambatDate(1999, 1, 1))).isFalse();
    }

    @Test
    void rejectsADayThatDoesNotExistInItsMonth() {
        // BS 2080 Baisakh has 31 days -- day 32 does not exist.
        assertThatThrownBy(() -> BikramSambatConverter.toAd(new BikramSambatDate(2080, 1, 32)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bikramSambatDateRejectsInvalidMonth() {
        assertThatThrownBy(() -> new BikramSambatDate(2080, 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BikramSambatDate(2080, 13, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * The strongest available correctness check: every single day in the
     * supported ~91-year range must round-trip AD -> BS -> AD back to
     * itself, with no gaps and no duplicate BS dates. This does not prove
     * the underlying calendar data is astronomically correct, but it does
     * prove the conversion algorithm itself (prefix-sum lookup, month/year
     * rollover) has no off-by-one errors anywhere across ~33,000 days.
     */
    @Test
    void everyDayInTheSupportedRangeRoundTripsExactly() {
        LocalDate current = BikramSambatConverter.MIN_SUPPORTED_AD_DATE;
        BikramSambatDate previousBs = null;
        long dayCount = 0;

        while (!current.isAfter(BikramSambatConverter.MAX_SUPPORTED_AD_DATE)) {
            BikramSambatDate bs = BikramSambatConverter.toBs(current);
            LocalDate roundTripped = BikramSambatConverter.toAd(bs);

            assertThat(roundTripped).as("round-trip for AD date %s", current).isEqualTo(current);

            if (previousBs != null) {
                assertThat(bs).as("BS date must strictly advance from the previous day").isNotEqualTo(previousBs);
            }

            previousBs = bs;
            current = current.plusDays(1);
            dayCount++;
        }

        assertThat(dayCount).isEqualTo(totalSupportedDayCount());
    }

    private static long totalSupportedDayCount() {
        return java.time.temporal.ChronoUnit.DAYS.between(
                BikramSambatConverter.MIN_SUPPORTED_AD_DATE, BikramSambatConverter.MAX_SUPPORTED_AD_DATE) + 1;
    }

    private static int monthLengthFor(int bsYear, int bsMonth) {
        // Walk forward from day 1 until toAd() would throw for day+1 -- avoids
        // depending on the table's package-private internals from the test.
        int day = 1;
        while (true) {
            try {
                BikramSambatConverter.toAd(new BikramSambatDate(bsYear, bsMonth, day + 1));
                day++;
            } catch (IllegalArgumentException ex) {
                return day;
            }
        }
    }
}
