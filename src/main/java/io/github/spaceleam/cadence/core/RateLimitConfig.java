package io.github.spaceleam.cadence.core;

import java.util.concurrent.TimeUnit;

/**
 * Konfigurasi untuk Rate Limiter.
 * Immutable class yang hold semua settings.
 */
public final class RateLimitConfig {

    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodNanos;

    public RateLimitConfig(int capacity, int refillTokens, long refillPeriod, TimeUnit unit) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity harus > 0, got: " + capacity);
        }
        if (refillTokens < 0) {
            throw new IllegalArgumentException("Refill tokens harus >= 0, got: " + refillTokens);
        }
        if (refillPeriod < 0) {
            throw new IllegalArgumentException("Refill period harus >= 0, got: " + refillPeriod);
        }

        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodNanos = unit.toNanos(refillPeriod);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public long getRefillPeriodNanos() {
        return refillPeriodNanos;
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "capacity=" + capacity +
                ", refillTokens=" + refillTokens +
                ", refillPeriodNanos=" + refillPeriodNanos +
                '}';
    }
}
