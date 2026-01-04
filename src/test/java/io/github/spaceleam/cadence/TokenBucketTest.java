package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests untuk TokenBucketLimiter.
 */
class TokenBucketTest {

    @Test
    @DisplayName("Bisa blokir request ke-4 kalau capacity cuma 3")
    void testBasicLimit() {
        // Setup: Ember cuma muat 3 token, NO refill
        RateLimiter limiter = Cadence.builder()
                .capacity(3)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        // Execution
        assertTrue(limiter.tryAcquire(), "Request 1 should succeed");
        assertTrue(limiter.tryAcquire(), "Request 2 should succeed");
        assertTrue(limiter.tryAcquire(), "Request 3 should succeed");
        assertFalse(limiter.tryAcquire(), "Request 4 should be BLOCKED");

        // Kalau try lagi, tetap blocked
        assertFalse(limiter.tryAcquire(), "Request 5 should still be blocked");
    }

    @Test
    @DisplayName("Harus tahan 1000 request sekaligus tanpa crash")
    void testBurstTraffic() {
        RateLimiter limiter = Cadence.builder()
                .capacity(100)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // Simulate 1000 user tekan tombol submit BARENG
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
    @DisplayName("50 thread akses barengan - harusnya gak ada race condition")
    void testThreadSafety() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(1000)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5000);

        // 50 thread, masing-masing 100 request = 5000 total request
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

        // Harus TEPAT 1000, gak boleh 1001 atau 999
        assertEquals(1000, successCount.get(), "Exactly 1000 requests should succeed");
    }

    @Test
    @DisplayName("Request berat consume lebih banyak token")
    void testWeightedRequests() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        // Light request (1 token)
        assertTrue(limiter.tryAcquire(1), "1 token request should succeed");
        assertEquals(9, limiter.getAvailableTokens());

        // Heavy request (5 token)
        assertTrue(limiter.tryAcquire(5), "5 token request should succeed");
        assertEquals(4, limiter.getAvailableTokens());

        // Another heavy request (5 token) - harusnya gagal
        assertFalse(limiter.tryAcquire(5), "5 token request should fail - only 4 available");

        // Tapi light request masih bisa
        assertTrue(limiter.tryAcquire(1), "1 token request should still work");
    }

    @Test
    @DisplayName("Token gak boleh melebihi capacity walau lama gak dipake")
    void testCapacityLimit() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(100, TimeUnit.SECONDS) // Very fast refill
                .build();

        // Habiskan semua token
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }
        assertEquals(0, limiter.getAvailableTokens());

        // Wait for refill (100 tokens/second, wait 200ms = should refill 20 tokens)
        Thread.sleep(200);

        // But should be capped at capacity (10)
        assertTrue(limiter.getAvailableTokens() <= 10,
                "Tokens should not exceed capacity");
    }

    @Test
    @DisplayName("Kalau refill = 0, token gak pernah nambah")
    void testStaticQuota() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(5)
                .refillRate(0, TimeUnit.SECONDS) // NO REFILL!
                .build();

        // Habiskan 5 token
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(), "Token " + (i + 1) + " should be available");
        }

        // Tunggu 100ms (no refill should happen)
        Thread.sleep(100);

        // Tetap gak bisa acquire
        assertFalse(limiter.tryAcquire(), "Should still be blocked - no refill");
        assertEquals(0, limiter.getAvailableTokens());
    }

    @Test
    @DisplayName("Reset harus kembalikan bucket ke full capacity")
    void testReset() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        // Habiskan semua token
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }
        assertEquals(0, limiter.getAvailableTokens());

        // Reset
        limiter.reset();

        // Should be full again
        assertEquals(10, limiter.getAvailableTokens());
    }

    @Test
    @DisplayName("Token refill setelah periode waktu")
    void testRefillWorks() throws InterruptedException {
        // 10 token, refill 10 token per detik
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(10, TimeUnit.SECONDS)
                .build();

        // Habiskan semua token
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire());
        }
        assertFalse(limiter.tryAcquire(), "Should be empty");

        // Tunggu 1 detik (should refill 10 tokens)
        Thread.sleep(1100); // Extra 100ms buffer

        // Harusnya bisa acquire lagi
        assertTrue(limiter.tryAcquire(), "Should have tokens after refill");
    }

    @Test
    @DisplayName("Exception thrown untuk invalid token amount")
    void testInvalidTokenAmount() {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            limiter.tryAcquire(0);
        }, "Should throw for 0 tokens");

        assertThrows(IllegalArgumentException.class, () -> {
            limiter.tryAcquire(-1);
        }, "Should throw for negative tokens");
    }

    @Test
    @DisplayName("Builder dengan default values harus work")
    void testDefaultBuilder() {
        RateLimiter limiter = Cadence.builder().build();

        // Default capacity adalah 10
        assertEquals(10, limiter.getAvailableTokens());

        assertTrue(limiter.tryAcquire());
    }

    @Test
    @DisplayName("Cadence version harus return string")
    void testVersion() {
        assertNotNull(Cadence.version());
        assertTrue(Cadence.version().contains("1.0.0"));
    }
}
