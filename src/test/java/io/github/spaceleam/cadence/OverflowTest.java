package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimiter;
import io.github.spaceleam.cadence.core.TokenBucketLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class OverflowTest {

    @Test
    @DisplayName("Reproduce Integer Overflow in availableTokens")
    void testAvailableTokensOverflow() throws Exception {
        // Create a limiter with capacity = Integer.MAX_VALUE
        RateLimiter limiter = Cadence.builder()
                .capacity(Integer.MAX_VALUE)
                .refillRate(10, TimeUnit.MILLISECONDS) // Refill 10 tokens every 10ms
                .build();

        // 1. Consume some tokens to make room for refill
        limiter.tryAcquire(100);

        // availableTokens should be Integer.MAX_VALUE - 100
        assertEquals(Integer.MAX_VALUE - 100, limiter.getAvailableTokens());

        // 2. Manipulate lastRefillTime to simulate time passing, forcing a refill
        // We want tokensToAdd to be at least 200, so current + tokensToAdd > Integer.MAX_VALUE

        // We need to access private fields to manipulate state directly for deterministic testing
        Field availableTokensField = TokenBucketLimiter.class.getDeclaredField("availableTokens");
        availableTokensField.setAccessible(true);
        AtomicInteger availableTokens = (AtomicInteger) availableTokensField.get(limiter);

        // Set available tokens to close to MAX_VALUE
        availableTokens.set(Integer.MAX_VALUE - 10);

        Field lastRefillTimeField = TokenBucketLimiter.class.getDeclaredField("lastRefillTime");
        lastRefillTimeField.setAccessible(true);
        AtomicLong lastRefillTime = (AtomicLong) lastRefillTimeField.get(limiter);

        // Set last refill time to be 1 second ago (100 * 10ms periods)
        // Refill is 10 tokens per period. So 100 periods * 10 tokens = 1000 tokens to add.
        // current (MAX - 10) + tokensToAdd (1000) = Overflow!
        long oneSecondAgo = System.nanoTime() - TimeUnit.SECONDS.toNanos(1);
        lastRefillTime.set(oneSecondAgo);

        // 3. Trigger refill by calling getAvailableTokens() or tryAcquire()
        int available = limiter.getAvailableTokens();

        // If bug exists, available will be negative due to overflow
        assertTrue(available > 0, "Available tokens should be positive, got: " + available);
        assertEquals(Integer.MAX_VALUE, available, "Should be capped at Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("Reproduce Long Overflow in periodsElapsed * refillTokens")
    void testRefillCalculationOverflow() throws Exception {
         // Create a limiter
        RateLimiter limiter = Cadence.builder()
                .capacity(100)
                .refillRate(Integer.MAX_VALUE, TimeUnit.NANOSECONDS) // Huge refill rate
                .build();

        // Manipulate lastRefillTime to be long ago
        Field lastRefillTimeField = TokenBucketLimiter.class.getDeclaredField("lastRefillTime");
        lastRefillTimeField.setAccessible(true);
        AtomicLong lastRefillTime = (AtomicLong) lastRefillTimeField.get(limiter);

        // Set last refill to 5 seconds ago
        // periodsElapsed = 5e9
        // refillTokens = 2e9
        // product = 10e18 > Long.MAX_VALUE (9e18) -> Overflow to negative
        long fiveSecondsAgo = System.nanoTime() - TimeUnit.SECONDS.toNanos(5);
        lastRefillTime.set(fiveSecondsAgo);

        // Drain tokens first so refill is needed
        // Note: Because refillRate is massive (MAX_INT per ns), any call to tryAcquire/getAvailableTokens
        // will trigger a refill if time has passed.
        // If the overflow fix works, the refill happens correctly.
        // If the bug exists, the calculation overflows, refill is skipped, and available tokens stay low.

        limiter.tryAcquire(100);

        // Trigger refill
        int available = limiter.getAvailableTokens();

        // If overflow happens, tokensToAdd becomes negative, refill skipped, available remains 0.
        // If fixed, it refills to capacity (100).
        assertEquals(100, available, "Should have refilled to capacity");
    }
}
