package io.github.spaceleam.cadence.builder;

import io.github.spaceleam.cadence.core.RateLimitConfig;
import io.github.spaceleam.cadence.core.RateLimiter;
import io.github.spaceleam.cadence.core.TokenBucketLimiter;

import java.util.concurrent.TimeUnit;

/**
 * Fluent Builder untuk bikin RateLimiter.
 * 
 * Usage:
 * 
 * <pre>
 * RateLimiter limiter = CadenceBuilder.create()
 *         .capacity(100)
 *         .refillRate(100, TimeUnit.MINUTES)
 *         .build();
 * </pre>
 */
public class CadenceBuilder {

    private int capacity = 10;
    private int refillTokens = 10;
    private long refillPeriod = 1;
    private TimeUnit refillUnit = TimeUnit.SECONDS;

    private CadenceBuilder() {
        // Private constructor - use create()
    }

    /**
     * Create new builder instance.
     */
    public static CadenceBuilder create() {
        return new CadenceBuilder();
    }

    /**
     * Set bucket capacity (max tokens).
     * 
     * @param capacity maximum number of tokens the bucket can hold
     * @return this builder for chaining
     */
    public CadenceBuilder capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Set refill rate - berapa token ditambah per periode.
     * 
     * @param tokens number of tokens to add per period
     * @param period the refill period (e.g., 10 tokens per 1 SECOND)
     * @param unit   time unit for the period
     * @return this builder for chaining
     */
    public CadenceBuilder refillRate(int tokens, long period, TimeUnit unit) {
        this.refillTokens = tokens;
        this.refillPeriod = period;
        this.refillUnit = unit;
        return this;
    }

    /**
     * Shorthand untuk refillRate dengan period = 1.
     * Contoh: refillRate(100, TimeUnit.MINUTES) = 100 token per menit
     * 
     * @param tokens number of tokens to add per unit time
     * @param unit   time unit
     * @return this builder for chaining
     */
    public CadenceBuilder refillRate(int tokens, TimeUnit unit) {
        return refillRate(tokens, 1, unit);
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
        return new TokenBucketLimiter(config);
    }
}
