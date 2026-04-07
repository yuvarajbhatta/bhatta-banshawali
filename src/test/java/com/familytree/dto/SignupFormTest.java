package com.familytree.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignupFormTest {

    @Test
    void passwordsMatchReturnsTrueWhenValuesMatch() {
        SignupForm form = new SignupForm();
        form.setPassword("secret123");
        form.setConfirmPassword("secret123");

        assertThat(form.passwordsMatch()).isTrue();
    }

    @Test
    void passwordsMatchReturnsFalseWhenValuesDiffer() {
        SignupForm form = new SignupForm();
        form.setPassword("secret123");
        form.setConfirmPassword("different123");

        assertThat(form.passwordsMatch()).isFalse();
    }
}
