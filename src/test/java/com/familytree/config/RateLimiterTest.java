package com.familytree.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void allowsUpToCapacityThenDenies() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume("test-scope", "1.2.3.4", 3, Duration.ofHours(1))).isTrue();
        }
        assertThat(rateLimiter.tryConsume("test-scope", "1.2.3.4", 3, Duration.ofHours(1))).isFalse();
    }

    @Test
    void tracksDifferentKeysIndependently() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume("test-scope", "1.1.1.1", 3, Duration.ofHours(1))).isTrue();
        }

        assertThat(rateLimiter.tryConsume("test-scope", "2.2.2.2", 3, Duration.ofHours(1))).isTrue();
    }

    @Test
    void tracksDifferentScopesIndependentlyForTheSameKey() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume("signup", "1.2.3.4", 3, Duration.ofHours(1))).isTrue();
        }

        assertThat(rateLimiter.tryConsume("login", "1.2.3.4", 3, Duration.ofHours(1))).isTrue();
    }
}
