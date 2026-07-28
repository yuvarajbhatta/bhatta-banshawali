package com.familytree.services;

/**
 * Who is looking, for the purposes of sensitive-field redaction
 * (docs/09-security-threat-model.md; birth date and current address are
 * hidden from other members -- see PersonProfileAssembler). Deliberately
 * a fixed rule, not the full per-field/per-person PrivacyPreference
 * classification described in docs/04-data-model.md's target schema --
 * that's a larger, separately-scoped build.
 */
public record ViewerContext(boolean isAdmin, Long viewerPersonId) {

    public boolean canSeeSensitiveFieldsFor(Long personId) {
        return isAdmin || (viewerPersonId != null && viewerPersonId.equals(personId));
    }
}
