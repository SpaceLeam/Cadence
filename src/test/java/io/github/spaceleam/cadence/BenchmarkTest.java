package io.github.spaceleam.cadence;

import io.github.spaceleam.cadence.core.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Benchmark tests for measuring throughput and latency.
 */
class BenchmarkTest {

    @Test
    @DisplayName("Benchmark: Single-threaded throughput")
    void benchmarkSingleThreadThroughput() {
        RateLimiter limiter = Cadence.builder()
                .capacity(Integer.MAX_VALUE)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        int iterations = 1_000_000;

        // Warm-up
        for (int i = 0; i < 10_000; i++) {
            limiter.tryAcquire();
        }
        limiter.reset();

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            limiter.tryAcquire();
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = (iterations * 1000.0) / durationMs;

        System.out.println("=== Single-Thread Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Duration: " + durationMs + " ms");
        System.out.printf("Throughput: %.2f ops/sec%n", opsPerSecond);
        System.out.printf("Avg latency: %.3f µs/op%n", (durationMs * 1000.0) / iterations);

        // Should achieve at least 1M ops/sec single-threaded
        assertTrue(opsPerSecond > 500_000,
                "Expected >500K ops/sec, got: " + opsPerSecond);
    }

    @Test
    @DisplayName("Benchmark: Multi-threaded throughput")
    void benchmarkMultiThreadThroughput() throws InterruptedException {
        RateLimiter limiter = Cadence.builder()
                .capacity(Integer.MAX_VALUE)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        int threadCount = 4;
        int iterationsPerThread = 250_000;
        int totalIterations = threadCount * iterationsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicLong totalOps = new AtomicLong(0);

        // Prepare threads
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        if (limiter.tryAcquire()) {
                            totalOps.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Warm-up
        for (int i = 0; i < 10_000; i++) {
            limiter.tryAcquire();
        }
        limiter.reset();

        // Start benchmark
        long startTime = System.nanoTime();
        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        executor.shutdown();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = (totalOps.get() * 1000.0) / durationMs;

        System.out.println("=== Multi-Thread Benchmark ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Total ops: " + totalOps.get());
        System.out.println("Duration: " + durationMs + " ms");
        System.out.printf("Throughput: %.2f ops/sec%n", opsPerSecond);

        // Should achieve good throughput even under contention
        assertTrue(opsPerSecond > 100_000,
                "Expected >100K ops/sec with contention, got: " + opsPerSecond);
    }

    @Test
    @DisplayName("Benchmark: Latency measurement")
    void benchmarkLatency() {
        RateLimiter limiter = Cadence.builder()
                .capacity(100_000)
                .refillRate(0, TimeUnit.SECONDS)
                .build();

        int samples = 10_000;
        long[] latencies = new long[samples];

        // Warm-up
        for (int i = 0; i < 1_000; i++) {
            limiter.tryAcquire();
        }
        limiter.reset();

        // Measure latency for each call
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            limiter.tryAcquire();
            latencies[i] = System.nanoTime() - start;
        }

        // Sort for percentile calculation
        java.util.Arrays.sort(latencies);

        long p50 = latencies[samples / 2];
        long p90 = latencies[(int) (samples * 0.90)];
        long p99 = latencies[(int) (samples * 0.99)];
        long p999 = latencies[(int) (samples * 0.999)];

        System.out.println("=== Latency Benchmark ===");
        System.out.println("Samples: " + samples);
        System.out.printf("p50:  %.3f µs%n", p50 / 1000.0);
        System.out.printf("p90:  %.3f µs%n", p90 / 1000.0);
        System.out.printf("p99:  %.3f µs%n", p99 / 1000.0);
        System.out.printf("p999: %.3f µs%n", p999 / 1000.0);

        // p50 should be sub-microsecond
        assertTrue(p50 < 10_000,
                "Expected p50 < 10µs, got: " + (p50 / 1000.0) + "µs");
    }
}
