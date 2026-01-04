package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.builder.CadenceBuilder;
import io.github.spaceleam.cadence.core.RateLimiter;

/**
 * Cadence - Lightweight In-Memory Rate Limiter
 * 
 * <p>
 * Entry point for the rate limiter library using Token Bucket algorithm.
 * </p>
 * 
 * <h2>Quick Start:</h2>
 * 
 * <pre>{@code
 * RateLimiter limiter = Cadence.builder()
 *         .capacity(100)
 *         .refillRate(100, TimeUnit.MINUTES)
 *         .build();
 * 
 * if (limiter.tryAcquire()) {
 *     // Process request
 * } else {
 *     throw new RateLimitException("Too many requests");
 * }
 * }</pre>
 * 
 * <h2>Preset Configurations:</h2>
 * 
 * <pre>{@code
 * RateLimiter loginLimiter = Cadence.forLogin(); // 5 req/min
 * RateLimiter apiLimiter = Cadence.forAPI(); // 100 req/sec
 * RateLimiter otpLimiter = Cadence.forOTP(); // 3 req/hour
 * }</pre>
 * 
 * @author SpaceLeam
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Cadence {

    private Cadence() {
    }

    /**
     * Create a new builder for custom configuration.
     * 
     * @return new builder instance
     */
    public static CadenceBuilder builder() {
        return CadenceBuilder.create();
    }

    // ========== PRESET FACTORY METHODS ==========

    /**
     * Preset for login endpoint (5 attempts per minute).
     * 
     * @return configured rate limiter
     */
    public static RateLimiter forLogin() {
        return CadenceBuilder.forLogin();
    }

    /**
     * Preset for OTP/SMS verification (3 requests per hour).
     * 
     * @return configured rate limiter
     */
    public static RateLimiter forOTP() {
        return CadenceBuilder.forOTP();
    }

    /**
     * Preset for standard API endpoint (100 requests per second).
     * 
     * @return configured rate limiter
     */
    public static RateLimiter forAPI() {
        return CadenceBuilder.forAPI();
    }

    /**
     * Preset for file downloads (10 downloads per hour).
     * 
     * @return configured rate limiter
     */
    public static RateLimiter forDownload() {
        return CadenceBuilder.forDownload();
    }

    /**
     * Preset for search/heavy queries (30 requests per minute).
     * 
     * @return configured rate limiter
     */
    public static RateLimiter forSearch() {
        return CadenceBuilder.forSearch();
    }

    /**
     * Library version.
     * 
     * @return version string
     */
    public static String version() {
        return "1.0.0";
    }
}
