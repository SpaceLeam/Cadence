package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimitResult;
import io.github.spaceleam.cadence.core.RateLimiter;
import io.github.spaceleam.cadence.core.RateLimiterListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenBucketLimiter.
 */
class TokenBucketTest {

    @Test
    @DisplayName("Block request after capacity exhausted")
    void testBasicLimit() {
        RateLimiter limiter = Cadence.builder()
                .capacity(3)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertTrue(limiter.tryAcquire(), "Request 1 should succeed");
        assertTrue(limiter.tryAcquire(), "Request 2 should succeed");
        assertTrue(limiter.tryAcquire(), "Request 3 should succeed");
        assertFalse(limiter.tryAcquire(), "Request 4 should be BLOCKED");
        assertFalse(limiter.tryAcquire(), "Request 5 should still be blocked");
    }

    @Test
    @DisplayName("Handle burst traffic without crash")
    void testBurstTraffic() {
        RateLimiter limiter = Cadence.builder()
                .capacity(100)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (int i = 0; i < 1000; i++) {
            if (limiter.tryAcquire()) {
                successCount.incrementAndGet();
            } else {
                failedCount.incrementAndGet();
            }
        }

        assertEquals(100, successCount.get(), "Only 100 requests should succeed");
        assertEquals(900, failedCount.get(), "900 requests should be blocked");
    }

    @Test
    @DisplayName("Thread-safe with concurrent access")
    void testThreadSafety() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(1000)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5000);

        for (int i = 0; i < 5000; i++) {
            executor.submit(() -> {
                try {
                    if (limiter.tryAcquire()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All tasks should complete");
        executor.shutdown();
        assertEquals(1000, successCount.get(), "Exactly 1000 requests should succeed");
    }

    @Test
    @DisplayName("Weighted token consumption")
    void testWeightedRequests() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertTrue(limiter.tryAcquire(1), "1 token request should succeed");
        assertEquals(9, limiter.getAvailableTokens());

        assertTrue(limiter.tryAcquire(5), "5 token request should succeed");
        assertEquals(4, limiter.getAvailableTokens());

        assertFalse(limiter.tryAcquire(5), "5 token request should fail");
        assertTrue(limiter.tryAcquire(1), "1 token request should still work");
    }

    @Test
    @DisplayName("Tokens should not exceed capacity")
    void testCapacityLimit() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(100, TimeUnit.SECONDS)
                .build();

        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }
        assertEquals(0, limiter.getAvailableTokens());

        Thread.sleep(200);

        assertTrue(limiter.getAvailableTokens() <= 10,
                "Tokens should not exceed capacity");
    }

    @Test
    @DisplayName("Static quota with zero refill")
    void testStaticQuota() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(5)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(), "Token " + (i + 1) + " should be available");
        }

        Thread.sleep(100);

        assertFalse(limiter.tryAcquire(), "Should still be blocked");
        assertEquals(0, limiter.getAvailableTokens());
    }

    @Test
    @DisplayName("Reset returns bucket to full capacity")
    void testReset() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }
        assertEquals(0, limiter.getAvailableTokens());

        limiter.reset();
        assertEquals(10, limiter.getAvailableTokens());
    }

    @Test
    @DisplayName("Token refill after time period")
    void testRefillWorks() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(10, TimeUnit.SECONDS)
                .build();

        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire());
        }
        assertFalse(limiter.tryAcquire(), "Should be empty");

        Thread.sleep(1100);

        assertTrue(limiter.tryAcquire(), "Should have tokens after refill");
    }

    @Test
    @DisplayName("Exception for invalid token amount")
    void testInvalidTokenAmount() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(0));
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(-1));
    }

    @Test
    @DisplayName("Builder with default values")
    void testDefaultBuilder() {
        RateLimiter limiter = Cadence.builder().build();
        assertEquals(10, limiter.getAvailableTokens());
        assertTrue(limiter.tryAcquire());
    }

    @Test
    @DisplayName("Version returns valid string")
    void testVersion() {
        assertNotNull(Cadence.version());
        assertTrue(Cadence.version().contains("1.0.0"));
    }

    // ========== NEW EDGE CASE TESTS ==========

    @Test
    @DisplayName("Builder rejects negative capacity")
    void testBuilderRejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> Cadence.builder().capacity(-100).build());
    }

    @Test
    @DisplayName("Builder rejects zero capacity")
    void testBuilderRejectsZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> Cadence.builder().capacity(0).build());
    }

    @Test
    @DisplayName("Builder rejects negative refill rate")
    void testBuilderRejectsNegativeRefillRate() {
        assertThrows(IllegalArgumentException.class, () -> Cadence.builder().refillRate(-50, TimeUnit.SECONDS).build());
    }

    @Test
    @DisplayName("Builder accepts Integer.MAX_VALUE capacity")
    void testMaxIntCapacity() {
        RateLimiter limiter = Cadence.builder()
                .capacity(Integer.MAX_VALUE)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertEquals(Integer.MAX_VALUE, limiter.getAvailableTokens());
        assertTrue(limiter.tryAcquire());
    }

    // ========== PRESET TESTS ==========

    @Test
    @DisplayName("forLogin preset works")
    void testForLoginPreset() {
        RateLimiter limiter = Cadence.forLogin();
        assertNotNull(limiter);
        assertEquals(5, limiter.getAvailableTokens());
    }

    @Test
    @DisplayName("forAPI preset works")
    void testForAPIPreset() {
        RateLimiter limiter = Cadence.forAPI();
        assertNotNull(limiter);
        assertEquals(100, limiter.getAvailableTokens());
    }

    @Test
    @DisplayName("forOTP preset works")
    void testForOTPPreset() {
        RateLimiter limiter = Cadence.forOTP();
        assertNotNull(limiter);
        assertEquals(3, limiter.getAvailableTokens());
    }

    // ========== TIMEOUT TESTS ==========

    @Test
    @DisplayName("tryAcquire with timeout succeeds")
    void testTryAcquireWithTimeoutSuccess() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertTrue(limiter.tryAcquire(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("tryAcquire with timeout fails after timeout")
    void testTryAcquireWithTimeoutExpires() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(1)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertTrue(limiter.tryAcquire()); // Drain
        assertFalse(limiter.tryAcquire(100, TimeUnit.MILLISECONDS));
    }

    // ========== RESULT INFO TESTS ==========

    @Test
    @DisplayName("tryAcquireWithInfo returns success result")
    void testTryAcquireWithInfoSuccess() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        RateLimitResult result = limiter.tryAcquireWithInfo();
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTokensRequested());
        assertEquals(9, result.getTokensAvailable());
        assertNull(result.getReason());
    }

    @Test
    @DisplayName("tryAcquireWithInfo returns rejected result")
    void testTryAcquireWithInfoRejected() {
        RateLimiter limiter = Cadence.builder()
                .capacity(1)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        limiter.tryAcquire(); // Drain
        RateLimitResult result = limiter.tryAcquireWithInfo();
        assertFalse(result.isSuccess());
        assertNotNull(result.getReason());
        assertTrue(result.getRetryAfterNanos() > 0 || result.getRetryAfterNanos() == Long.MAX_VALUE);
    }

    // ========== LISTENER TESTS ==========

    @Test
    @DisplayName("Listener receives acquire events")
    void testListenerOnAcquire() {
        AtomicInteger acquireCount = new AtomicInteger(0);

        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .listener(new RateLimiterListener() {
                    @Override
                    public void onAcquire(int tokens) {
                        acquireCount.addAndGet(tokens);
                    }

                    @Override
                    public void onReject(int requested, int available) {
                    }
                })
                .build();

        limiter.tryAcquire();
        limiter.tryAcquire(3);

        assertEquals(4, acquireCount.get());
    }

    @Test
    @DisplayName("Listener receives reject events")
    void testListenerOnReject() {
        AtomicInteger rejectCount = new AtomicInteger(0);

        RateLimiter limiter = Cadence.builder()
                .capacity(1)
                .refillRate(0, TimeUnit.SECONDS)
                .listener(new RateLimiterListener() {
                    @Override
                    public void onAcquire(int tokens) {
                    }

                    @Override
                    public void onReject(int requested, int available) {
                        rejectCount.incrementAndGet();
                    }
                })
                .build();

        limiter.tryAcquire(); // Success
        limiter.tryAcquire(); // Reject
        limiter.tryAcquire(); // Reject

        assertEquals(2, rejectCount.get());
    }
}
