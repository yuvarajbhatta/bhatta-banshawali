package com.familytree.services;

import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * Normalizes and compares person names for the signup family-match engine
 * (docs/05-auth-and-verification.md). Handles case, whitespace,
 * punctuation, and Unicode normalization, plus English<->Nepali
 * transliteration so "Yuva Bhatta" matches "युव भट्ट" when one is stored
 * in English and the other in Nepali.
 *
 * Does not yet handle alias/nickname tables (docs lists this as a future
 * enhancement -- no Alias entity exists yet) or common honorific
 * prefix/suffix stripping beyond what NameTransliterationService already
 * normalizes for the "Bhatta" clan name specifically.
 */
@Service
public class NameMatcher {

    private final NameTransliterationService transliterationService;

    public NameMatcher(NameTransliterationService transliterationService) {
        this.transliterationService = transliterationService;
    }

    /**
     * True if two names plausibly refer to the same person, comparing
     * directly and via English->Nepali transliteration in both directions.
     */
    public boolean namesMatch(String a, String b) {
        String normalizedA = normalize(a);
        String normalizedB = normalize(b);

        if (normalizedA.isEmpty() || normalizedB.isEmpty()) {
            return false;
        }
        if (normalizedA.equals(normalizedB)) {
            return true;
        }

        String transliteratedA = normalize(transliterationService.transliterate(a));
        String transliteratedB = normalize(transliterationService.transliterate(b));

        return transliteratedA.equals(normalizedB) || normalizedA.equals(transliteratedB);
    }

    public String normalize(String name) {
        if (name == null) {
            return "";
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFC)
                .toLowerCase()
                .trim()
                .replaceAll("[^\\p{L}\\p{N}\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized;
    }
}
