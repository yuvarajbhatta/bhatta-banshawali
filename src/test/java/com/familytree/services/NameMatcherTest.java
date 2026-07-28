package com.familytree.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameMatcherTest {

    private final NameMatcher nameMatcher = new NameMatcher(new NameTransliterationService());

    @Test
    void exactMatchIgnoringCase() {
        assertThat(nameMatcher.namesMatch("Yuva Bhatta", "yuva bhatta")).isTrue();
    }

    @Test
    void matchIgnoresExtraWhitespaceAndPunctuation() {
        assertThat(nameMatcher.namesMatch("Yuva  Bhatta", "Yuva, Bhatta.")).isTrue();
    }

    @Test
    void differentNamesDoNotMatch() {
        assertThat(nameMatcher.namesMatch("Yuva Bhatta", "Bhoj Bhatta")).isFalse();
    }

    @Test
    void blankOrNullNamesNeverMatch() {
        assertThat(nameMatcher.namesMatch("", "")).isFalse();
        assertThat(nameMatcher.namesMatch(null, "Yuva Bhatta")).isFalse();
        assertThat(nameMatcher.namesMatch("Yuva Bhatta", null)).isFalse();
    }

    @Test
    void matchesAcrossEnglishToNepaliTransliteration() {
        // NameTransliterationService transliterates "Bhatta" to भट्ट specifically.
        assertThat(nameMatcher.namesMatch("Bhatta", "भट्ट")).isTrue();
    }

    @Test
    void normalizeStripsPunctuationAndCollapsesWhitespace() {
        assertThat(nameMatcher.normalize("  Yuva,  Bhatta.  ")).isEqualTo("yuva bhatta");
    }
}
