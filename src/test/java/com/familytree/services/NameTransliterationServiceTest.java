package com.familytree.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameTransliterationServiceTest {

    private final NameTransliterationService service = new NameTransliterationService();

    /**
     * Regression test: "Bhatta" (the family's own clan name) previously
     * never transliterated correctly. The DIGRAPHS special-case for it
     * was keyed on "Bhatta" (capitalized) but looked up against an
     * already-lowercased string, and was inserted after the shorter "bh"
     * digraph, which matched first and consumed just the prefix -- so
     * "Bhatta" produced "भअततअ" instead of "भट्ट", silently, since this
     * class had no test coverage at all before now.
     */
    @Test
    void transliteratesBhattaCorrectlyRegardlessOfCase() {
        assertThat(service.transliterate("Bhatta")).isEqualTo("भट्ट");
        assertThat(service.transliterate("bhatta")).isEqualTo("भट्ट");
        assertThat(service.transliterate("BHATTA")).isEqualTo("भट्ट");
    }

    @Test
    void transliteratesBhattaWithinALongerName() {
        assertThat(service.transliterate("Bhatta Nath")).isEqualTo("भट्ट नाथ");
    }

    @Test
    void nullInputReturnsNull() {
        assertThat(service.transliterate(null)).isNull();
    }

    @Test
    void blankInputReturnsEmptyString() {
        assertThat(service.transliterate("   ")).isEmpty();
    }

    @Test
    void nonLetterCharactersPassThroughUnchanged() {
        assertThat(service.transliterate("123")).isEqualTo("123");
    }
}
