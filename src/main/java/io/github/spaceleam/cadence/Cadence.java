package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.builder.CadenceBuilder;

/**
 * Cadence - Lightweight In-Memory Rate Limiter
 * 
 * Entry point utama untuk library ini.
 * Menggunakan Token Bucket algorithm untuk rate limiting.
 * 
 * Quick Start:
 * 
 * <pre>
 * RateLimiter limiter = Cadence.builder()
 *         .capacity(100)
 *         .refillRate(100, TimeUnit.MINUTES)
 *         .build();
 * 
 * if (limiter.tryAcquire()) {
 *     // Process request
 * } else {
 *     throw new RateLimitException("Too many requests!");
 * }
 * </pre>
 * 
 * @author SpaceLeam
 * @version 1.0.0
 */
public final class Cadence {

    private Cadence() {
        // Utility class - no instantiation
    }

    /**
     * Create a new CadenceBuilder for fluent configuration.
     * 
     * @return new builder instance
     */
    public static CadenceBuilder builder() {
        return CadenceBuilder.create();
    }

    /**
     * Library version.
     */
    public static String version() {
        return "1.0.0-SNAPSHOT";
    }
}
