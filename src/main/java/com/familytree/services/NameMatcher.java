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

    /**
     * Like namesMatch(), but distinguishes an exact/transliteration match
     * (EXACT) from a same-script spelling-variant match (FUZZY -- e.g.
     * "Yuvraj"/"Yuvaraj", "Bhojraj"/"Bhoj Raj") from no match at all (NONE).
     * Only FamilyMatchService's signup matcher consumes FUZZY today --
     * namesMatch() itself is deliberately left untouched above so
     * DuplicateCandidateService's admin duplicate-detection doesn't
     * silently become fuzzy too.
     */
    public NameMatchQuality matchQuality(String a, String b) {
        String normalizedA = normalize(a);
        String normalizedB = normalize(b);

        if (normalizedA.isEmpty() || normalizedB.isEmpty()) {
            return NameMatchQuality.NONE;
        }
        if (normalizedA.equals(normalizedB)) {
            return NameMatchQuality.EXACT;
        }

        String transliteratedA = normalize(transliterationService.transliterate(a));
        String transliteratedB = normalize(transliterationService.transliterate(b));
        if (transliteratedA.equals(normalizedB) || normalizedA.equals(transliteratedB)) {
            return NameMatchQuality.EXACT;
        }

        return isFuzzyMatch(normalizedA, normalizedB) ? NameMatchQuality.FUZZY : NameMatchQuality.NONE;
    }

    // Below this length, fuzzing is too risky -- e.g. "Ram" vs "Sam" is only
    // one edit apart despite being two clearly different names.
    //
    // Kept deliberately tight: a real family tree where everyone shares one
    // surname (confirmed against this app's own seed data -- 2000+ Persons,
    // all "Bhatta", drawn from a pool of only ~30-50 recurring first names)
    // means a shared suffix contributes nothing to the edit-distance
    // calculation, so the effective comparison is really just between the
    // given names. A looser ratio/distance here (previously 0.2 / 3) let
    // dozens of unrelated same-generation people with similarly-short
    // first names all fuzzy-match at once, which isn't just noisy -- it
    // overflowed the VARCHAR(500) candidate-ID columns outright in a real
    // signup against this seed data. Distance 1 still catches the target
    // cases ("Yuvraj"/"Yuvaraj", "Bhojraj"/"Bhoj Raj" are each exactly one
    // edit apart) while being far less likely to coincidentally catch two
    // genuinely different short names.
    private static final int MIN_FUZZY_LENGTH = 4;
    private static final double FUZZY_DISTANCE_RATIO = 0.1;
    private static final int MAX_FUZZY_DISTANCE = 1;

    private boolean isFuzzyMatch(String normalizedA, String normalizedB) {
        if (normalizedA.length() < MIN_FUZZY_LENGTH || normalizedB.length() < MIN_FUZZY_LENGTH) {
            return false;
        }
        int maxLength = Math.max(normalizedA.length(), normalizedB.length());
        int allowedDistance = Math.min(MAX_FUZZY_DISTANCE, (int) Math.ceil(maxLength * FUZZY_DISTANCE_RATIO));
        return levenshteinDistance(normalizedA, normalizedB) <= allowedDistance;
    }

    private static int levenshteinDistance(String a, String b) {
        int[] previousRow = new int[b.length() + 1];
        int[] currentRow = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previousRow[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            currentRow[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                currentRow[j] = Math.min(Math.min(currentRow[j - 1] + 1, previousRow[j] + 1), previousRow[j - 1] + cost);
            }
            System.arraycopy(currentRow, 0, previousRow, 0, currentRow.length);
        }
        return previousRow[b.length()];
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
