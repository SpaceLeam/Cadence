package io.github.spaceleam.cadence.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementasi Token Bucket Algorithm.
 * 
 * Thread-safe menggunakan AtomicInteger dan AtomicLong.
 * Gak butuh synchronized atau lock - pure lock-free implementation.
 * 
 * Cara kerja:
 * - Bucket mulai dengan capacity token
 * - Setiap request consume 1 (atau lebih) token
 * - Bucket di-refill secara periodik berdasarkan config
 * - Kalau bucket kosong, request ditolak (fail-fast)
 */
public class TokenBucketLimiter implements RateLimiter {

    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodNanos;

    private final AtomicInteger availableTokens;
    private final AtomicLong lastRefillTime;

    public TokenBucketLimiter(RateLimitConfig config) {
        this.capacity = config.getCapacity();
        this.refillTokens = config.getRefillTokens();
        this.refillPeriodNanos = config.getRefillPeriodNanos();
        this.availableTokens = new AtomicInteger(capacity);
        this.lastRefillTime = new AtomicLong(System.nanoTime());
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(int tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens harus > 0, got: " + tokens);
        }

        // Refill dulu kalau waktunya
        refill();

        // Atomic compare-and-swap loop
        while (true) {
            int current = availableTokens.get();

            if (current < tokens) {
                return false; // Not enough tokens
            }

            if (availableTokens.compareAndSet(current, current - tokens)) {
                return true; // Successfully acquired
            }
            // CAS failed, another thread modified - retry
        }
    }

    @Override
    public int getAvailableTokens() {
        refill();
        return availableTokens.get();
    }

    @Override
    public void reset() {
        availableTokens.set(capacity);
        lastRefillTime.set(System.nanoTime());
    }

    /**
     * Refill tokens berdasarkan waktu yang sudah berlalu.
     * 
     * Method ini thread-safe menggunakan CAS operation.
     * Kalau refillPeriodNanos = 0 (no refill), method ini jadi no-op.
     */
    private void refill() {
        if (refillPeriodNanos == 0 || refillTokens == 0) {
            return; // No refill configured
        }

        long now = System.nanoTime();
        long lastRefill = lastRefillTime.get();
        long timeSinceLastRefill = now - lastRefill;

        // Hitung berapa periode yang sudah lewat
        if (timeSinceLastRefill >= refillPeriodNanos) {
            long periodsElapsed = timeSinceLastRefill / refillPeriodNanos;
            int tokensToAdd = (int) Math.min(periodsElapsed * refillTokens, capacity);

            if (tokensToAdd > 0) {
                // Try to claim the refill
                long newRefillTime = lastRefill + (periodsElapsed * refillPeriodNanos);

                if (lastRefillTime.compareAndSet(lastRefill, newRefillTime)) {
                    // We won the race - add tokens (capped at capacity)
                    availableTokens.updateAndGet(current -> Math.min(capacity, current + tokensToAdd));
                }
                // Else: another thread already did the refill, that's fine
            }
        }
    }

    /**
     * Get capacity (max tokens).
     */
    public int getCapacity() {
        return capacity;
    }
}
