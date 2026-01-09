package io.github.spaceleam.cadence.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenBucketOverflowTest {

    @Test
    @DisplayName("Verify refill calculation does not overflow with large time delta")
    void testRefillOverflow() throws Exception {
        // Setup a limiter
        RateLimitConfig config = new RateLimitConfig(10, 0, 1, TimeUnit.SECONDS);
        TokenBucketLimiter limiter = new TokenBucketLimiter(config);

        // 1. Drain the bucket
        while (limiter.tryAcquire()) {}
        assertEquals(0, limiter.getAvailableTokens(), "Bucket should be empty initially");

        // 2. Modify internal state to simulate large refill rate and small period
        Field refillTokensField = TokenBucketLimiter.class.getDeclaredField("refillTokens");
        refillTokensField.setAccessible(true);
        int refillTokens = 10;
        refillTokensField.setInt(limiter, refillTokens);

        Field refillPeriodNanosField = TokenBucketLimiter.class.getDeclaredField("refillPeriodNanos");
        refillPeriodNanosField.setAccessible(true);
        long refillPeriodNanos = 1L;
        refillPeriodNanosField.setLong(limiter, refillPeriodNanos);

        // 3. Simulate a massive time jump that would cause long overflow if multiplied naively
        // 3e18 * 10 = 30e18 > 9.22e18 (Long.MAX_VALUE)
        long delta = 3_000_000_000_000_000_000L;

        Field lastRefillTimeField = TokenBucketLimiter.class.getDeclaredField("lastRefillTime");
        lastRefillTimeField.setAccessible(true);
        AtomicLong lastRefillTime = (AtomicLong) lastRefillTimeField.get(limiter);

        long now = System.nanoTime();
        lastRefillTime.set(now - delta);

        // 4. Try to acquire.
        // Logic should detect that potential tokensToAdd exceeds capacity and cap it,
        // preventing the overflow calculation which would result in negative numbers.
        boolean acquired = limiter.tryAcquire();

        assertTrue(acquired, "Should be able to acquire token. Refill calculation should have capped at capacity instead of overflowing to negative.");
    }
}
