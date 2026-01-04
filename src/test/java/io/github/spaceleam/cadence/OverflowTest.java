package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimiter;
import io.github.spaceleam.cadence.core.TokenBucketLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverflowTest {

    @Test
    @DisplayName("Verify no integer overflow during refill with large values")
    void testRefillOverflow() throws Exception {
        // Setup: Max tokens refill per nanosecond
        int maxInt = Integer.MAX_VALUE;
        RateLimiter limiter = Cadence.builder()
                .capacity(maxInt)
                .refillRate(maxInt, 1, TimeUnit.NANOSECONDS)
                .build();

        // Consume all tokens
        assertTrue(limiter.tryAcquire(maxInt));

        // Use reflection to simulate time passing to avoid waiting in test
        Field lastRefillField = TokenBucketLimiter.class.getDeclaredField("lastRefillTime");
        lastRefillField.setAccessible(true);
        AtomicLong lastRefillTime = (AtomicLong) lastRefillField.get(limiter);

        // Set last refill time to 5 seconds ago
        long now = System.nanoTime();
        lastRefillTime.set(now - TimeUnit.SECONDS.toNanos(5));

        // Trigger refill check
        int available = limiter.getAvailableTokens();

        // Should be full capacity
        assertEquals(maxInt, available, "Tokens should be refilled to capacity without overflow");
    }
}
