package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimitConfig;
import io.github.spaceleam.cadence.core.TokenBucketLimiter;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenBucketOverflowTest {
    @Test
    void testOverflow() throws Exception {
        // We need a large refillTokens and capacity to demonstrate overflow.
        // capacity = Integer.MAX_VALUE
        // refillTokens = Integer.MAX_VALUE
        // refillPeriod = 1ns

        RateLimitConfig config = new RateLimitConfig(Integer.MAX_VALUE, Integer.MAX_VALUE, 1, TimeUnit.NANOSECONDS);
        TokenBucketLimiter limiter = new TokenBucketLimiter(config);

        // Use reflection to set lastRefillTime to long ago
        Field lastRefillTimeField = TokenBucketLimiter.class.getDeclaredField("lastRefillTime");
        lastRefillTimeField.setAccessible(true);
        AtomicLong lastRefillTime = (AtomicLong) lastRefillTimeField.get(limiter);

        // We want periodsElapsed * refillTokens to overflow Long.MAX_VALUE
        // periodsElapsed = timeSinceLastRefill / 1
        // refillTokens = Integer.MAX_VALUE (~2e9)
        // We need periodsElapsed > Long.MAX_VALUE / 2e9 ~ 4e9

        // 4e9 ns is only 4 seconds.
        // So if we set lastRefillTime to 10 seconds ago, it should overflow.

        long now = System.nanoTime();
        long tenSecondsAgo = now - 10_000_000_000L;
        lastRefillTime.set(tenSecondsAgo);

        // Drain tokens first to ensure refill logic runs and tries to add tokens
        Field availableTokensField = TokenBucketLimiter.class.getDeclaredField("availableTokens");
        availableTokensField.setAccessible(true);
        AtomicInteger availableTokens = (AtomicInteger) availableTokensField.get(limiter);
        availableTokens.set(0); // Empty bucket

        // Just call getAvailableTokens, which triggers refill.
        int available = limiter.getAvailableTokens();
        System.out.println("Available tokens: " + available);

        // It should be full (Integer.MAX_VALUE)
        assertEquals(Integer.MAX_VALUE, available, "Bucket should be full after 10 seconds");
    }
}
