package com.familytree.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, per-instance rate limiting (docs/09-security-threat-model.md,
 * risks 1 and 2: credential stuffing and enumeration). Buckets never
 * expire and are lost on restart -- acceptable for a single-instance
 * deployment where this is a defense-in-depth abuse throttle, not the
 * only control (see also nginx and the anti-enumeration response-shape
 * guarantees in docs/05-auth-and-verification.md).
 */
@Component
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String scope, String key, int capacity, Duration period) {
        Bucket bucket = buckets.computeIfAbsent(scope + ":" + key, ignored -> newBucket(capacity, period));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }
}
