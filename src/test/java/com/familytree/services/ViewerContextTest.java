package com.familytree.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewerContextTest {

    @Test
    void adminCanSeeSensitiveFieldsForAnyone() {
        ViewerContext viewer = new ViewerContext(true, null);

        assertThat(viewer.canSeeSensitiveFieldsFor(1L)).isTrue();
        assertThat(viewer.canSeeSensitiveFieldsFor(999L)).isTrue();
    }

    @Test
    void nonAdminCanSeeSensitiveFieldsOnlyForTheirOwnLinkedPerson() {
        ViewerContext viewer = new ViewerContext(false, 42L);

        assertThat(viewer.canSeeSensitiveFieldsFor(42L)).isTrue();
        assertThat(viewer.canSeeSensitiveFieldsFor(43L)).isFalse();
    }

    @Test
    void unlinkedNonAdminCanSeeNoOnesSensitiveFields() {
        ViewerContext viewer = new ViewerContext(false, null);

        assertThat(viewer.canSeeSensitiveFieldsFor(1L)).isFalse();
    }
}
