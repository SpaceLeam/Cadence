package io.github.spaceleam.cadence.builder;

import io.github.spaceleam.cadence.core.RateLimitConfig;
import io.github.spaceleam.cadence.core.RateLimiter;
import io.github.spaceleam.cadence.core.RateLimiterListener;
import io.github.spaceleam.cadence.core.TokenBucketLimiter;

import java.util.concurrent.TimeUnit;

/**
 * Fluent Builder for creating RateLimiter instances.
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * RateLimiter limiter = CadenceBuilder.create()
 *         .capacity(100)
 *         .refillRate(100, TimeUnit.MINUTES)
 *         .build();
 * }</pre>
 * 
 * @since 1.0.0
 */
public class CadenceBuilder {

    private int capacity = 10;
    private int refillTokens = 10;
    private long refillPeriod = 1;
    private TimeUnit refillUnit = TimeUnit.SECONDS;
    private RateLimiterListener listener = null;

    private CadenceBuilder() {
    }

    /**
     * Create new builder instance.
     * 
     * @return new builder
     */
    public static CadenceBuilder create() {
        return new CadenceBuilder();
    }

    // ========== PRESET CONFIGS ==========

    /**
     * Preset for login endpoint protection.
     * Config: 5 attempts per minute.
     * 
     * @return configured limiter for login
     */
    public static RateLimiter forLogin() {
        return create()
                .capacity(5)
                .refillRate(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Preset for OTP/SMS verification.
     * Config: 3 requests per hour.
     * 
     * @return configured limiter for OTP
     */
    public static RateLimiter forOTP() {
        return create()
                .capacity(3)
                .refillRate(3, TimeUnit.HOURS)
                .build();
    }

    /**
     * Preset for standard API endpoint.
     * Config: 100 requests per second.
     * 
     * @return configured limiter for API
     */
    public static RateLimiter forAPI() {
        return create()
                .capacity(100)
                .refillRate(100, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Preset for file download endpoint.
     * Config: 10 downloads per hour.
     * 
     * @return configured limiter for downloads
     */
    public static RateLimiter forDownload() {
        return create()
                .capacity(10)
                .refillRate(10, TimeUnit.HOURS)
                .build();
    }

    /**
     * Preset for search/heavy query endpoint.
     * Config: 30 requests per minute.
     * 
     * @return configured limiter for search
     */
    public static RateLimiter forSearch() {
        return create()
                .capacity(30)
                .refillRate(30, TimeUnit.MINUTES)
                .build();
    }

    // ========== BUILDER METHODS ==========

    /**
     * Set bucket capacity (max tokens).
     * 
     * @param capacity maximum tokens, must be greater than 0
     * @return this builder for chaining
     * @throws IllegalArgumentException if capacity is not positive
     */
    public CadenceBuilder capacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0, got: " + capacity);
        }
        this.capacity = capacity;
        return this;
    }

    /**
     * Set refill rate.
     * 
     * @param tokens number of tokens to add per period, must be non-negative
     * @param period the refill period, must be non-negative
     * @param unit   time unit for the period
     * @return this builder for chaining
     * @throws IllegalArgumentException if tokens or period is negative
     */
    public CadenceBuilder refillRate(int tokens, long period, TimeUnit unit) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Refill tokens must be >= 0, got: " + tokens);
        }
        if (period < 0) {
            throw new IllegalArgumentException("Refill period must be >= 0, got: " + period);
        }
        if (unit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        this.refillTokens = tokens;
        this.refillPeriod = period;
        this.refillUnit = unit;
        return this;
    }

    /**
     * Shorthand for refillRate with period = 1.
     * Example: refillRate(100, TimeUnit.MINUTES) = 100 tokens per minute
     * 
     * @param tokens number of tokens per time unit
     * @param unit   time unit
     * @return this builder for chaining
     */
    public CadenceBuilder refillRate(int tokens, TimeUnit unit) {
        return refillRate(tokens, 1, unit);
    }

    /**
     * Set a listener for rate limiter events.
     * 
     * @param listener the listener to receive events
     * @return this builder for chaining
     */
    public CadenceBuilder listener(RateLimiterListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Build the RateLimiter instance.
     * 
     * @return configured RateLimiter
     */
    public RateLimiter build() {
        RateLimitConfig config = new RateLimitConfig(
                capacity,
                refillTokens,
                refillPeriod,
                refillUnit);
        return new TokenBucketLimiter(config, listener);
    }
}
