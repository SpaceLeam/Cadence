package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimitConfig;
import io.github.spaceleam.cadence.core.TokenBucketLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverflowTest {

    @Test
    @DisplayName("Refill calculation should not overflow with large parameters")
    void testRefillOverflow() throws Exception {
        // Setup: capacity 100, refill 2B tokens every 1 nanosecond.
        int capacity = 100;
        int refillTokens = 2_000_000_000; // ~2 billion
        long refillPeriod = 1; // 1 nanosecond
        TimeUnit unit = TimeUnit.NANOSECONDS;

        RateLimitConfig config = new RateLimitConfig(capacity, refillTokens, refillPeriod, unit);
        TokenBucketLimiter limiter = new TokenBucketLimiter(config);

        // Consume all tokens initially using reflection to avoid infinite loop
        Field availableTokensField = TokenBucketLimiter.class.getDeclaredField("availableTokens");
        availableTokensField.setAccessible(true);
        AtomicInteger availableTokens = (AtomicInteger) availableTokensField.get(limiter);
        availableTokens.set(0);

        // Verify it is empty using direct access (refill() would fill it immediately)
        assertEquals(0, availableTokens.get(), "Bucket should be empty");

        // Simulate 5 seconds passing
        // periodsElapsed = 5,000,000,000
        // product = 5e9 * 2e9 = 1e19 > Long.MAX_VALUE (9e18)

        Field lastRefillTimeField = TokenBucketLimiter.class.getDeclaredField("lastRefillTime");
        lastRefillTimeField.setAccessible(true);
        AtomicLong lastRefillTime = (AtomicLong) lastRefillTimeField.get(limiter);

        long now = System.nanoTime();
        // Set last refill to 5 seconds ago
        lastRefillTime.set(now - 5_000_000_000L);

        // Trigger refill by checking available tokens
        int available = limiter.getAvailableTokens();

        // Should be full (100)
        // If overflow happens, calculation becomes negative, tokensToAdd <= 0, so no refill -> stays 0
        assertEquals(100, available, "Bucket should be full after long pause, but overflow prevented it");
    }
}
