package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress tests untuk concurrency dan memory.
 */
class ConcurrencyTest {

    @RepeatedTest(3)
    @DisplayName("100 thread stress test - verify exact token count")
    void testHighConcurrencyStress() throws InterruptedException {
        final int CAPACITY = 500;
        final int THREAD_COUNT = 100;
        final int REQUESTS_PER_THREAD = 100;
        final int TOTAL_REQUESTS = THREAD_COUNT * REQUESTS_PER_THREAD;

        RateLimiter limiter = Cadence.builder()
                .capacity(CAPACITY)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1); // All threads start together
        CountDownLatch endLatch = new CountDownLatch(TOTAL_REQUESTS);

        // Prepare all tasks
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    if (limiter.tryAcquire()) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire!
        startLatch.countDown();

        assertTrue(endLatch.await(30, TimeUnit.SECONDS), "All tasks should complete");
        executor.shutdown();

        assertEquals(CAPACITY, successCount.get(),
                "Exactly " + CAPACITY + " requests should succeed, got: " + successCount.get());
    }

    @Test
    @DisplayName("User A spamming gak boleh ganggu User B")
    void testUserIsolation() {
        Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

        Function<String, RateLimiter> createLimiter = userId -> Cadence.builder()
                .capacity(3)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        // User A spamming 10x
        for (int i = 0; i < 10; i++) {
            limiters.computeIfAbsent("userA", createLimiter).tryAcquire();
        }

        // User A should be exhausted
        RateLimiter userALimiter = limiters.get("userA");
        assertFalse(userALimiter.tryAcquire(), "User A should be rate limited");

        // User B harusnya tetap bisa akses (fresh limiter)
        RateLimiter userBLimiter = limiters.computeIfAbsent("userB", createLimiter);
        assertTrue(userBLimiter.tryAcquire(), "User B request 1 should succeed");
        assertTrue(userBLimiter.tryAcquire(), "User B request 2 should succeed");
        assertTrue(userBLimiter.tryAcquire(), "User B request 3 should succeed");
        assertFalse(userBLimiter.tryAcquire(), "User B request 4 should fail");
    }

    @Test
    @DisplayName("Bikin 1000 limiter instances - basic memory check")
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest GC before measurement

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Bikin 1000 rate limiter instances
        List<RateLimiter> limiters = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            limiters.add(Cadence.builder()
                    .capacity(100)
                    .refillRate(10, TimeUnit.SECONDS)
                    .build());
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedBytes = memoryAfter - memoryBefore;
        long memoryUsedKB = memoryUsedBytes / 1024;

        // Each limiter should use < 1KB (1000 limiters = max ~1MB reasonable)
        assertTrue(memoryUsedKB < 5000,
                "Memory usage too high: " + memoryUsedKB + "KB for 1000 instances");

        // Verify all limiters work
        for (RateLimiter limiter : limiters) {
            assertTrue(limiter.tryAcquire());
        }
    }

    @Test
    @DisplayName("Concurrent refill tidak double-count")
    void testConcurrentRefill() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(100)
                .refillRate(50, TimeUnit.SECONDS)
                .build();

        // Habiskan semua token
        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire();
        }
        assertEquals(0, limiter.getAvailableTokens());

        // Tunggu 1 detik (seharusnya refill 50 token)
        Thread.sleep(1100);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(100);

        // 100 concurrent requests right after refill
        for (int i = 0; i < 100; i++) {
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Should be around 50 (±5 for timing tolerance)
        assertTrue(successCount.get() >= 45 && successCount.get() <= 55,
                "Expected ~50 successful requests, got: " + successCount.get());
    }

    @Test
    @DisplayName("Rapid acquire-reset cycle tidak race condition")
    void testRapidResetCycle() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(10)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            final int iteration = i;
            executor.submit(() -> {
                try {
                    if (iteration % 100 == 0) {
                        limiter.reset();
                    }
                    limiter.tryAcquire();

                    // Verify invariant: available tokens never negative
                    int available = limiter.getAvailableTokens();
                    if (available < 0) {
                        errors.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, errors.get(), "No negative token counts should occur");
    }
}
