package io.github.spaceleam.cadence.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket Algorithm implementation.
 * 
 * <p>
 * Thread-safe using atomic operations (lock-free).
 * </p>
 * 
 * <p>
 * How it works:
 * </p>
 * <ul>
 * <li>Bucket starts with capacity tokens</li>
 * <li>Each request consumes 1 or more tokens</li>
 * <li>Tokens are refilled periodically based on config</li>
 * <li>Requests are rejected when bucket is empty (fail-fast)</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public class TokenBucketLimiter implements RateLimiter {

    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodNanos;

    private final AtomicInteger availableTokens;
    private final AtomicLong lastRefillTime;
    private final RateLimiterListener listener;

    /**
     * Create limiter without listener.
     */
    public TokenBucketLimiter(RateLimitConfig config) {
        this(config, null);
    }

    /**
     * Create limiter with optional listener.
     */
    public TokenBucketLimiter(RateLimitConfig config, RateLimiterListener listener) {
        this.capacity = config.getCapacity();
        this.refillTokens = config.getRefillTokens();
        this.refillPeriodNanos = config.getRefillPeriodNanos();
        this.availableTokens = new AtomicInteger(capacity);
        this.lastRefillTime = new AtomicLong(System.nanoTime());
        this.listener = listener;
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(int tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens must be > 0, got: " + tokens);
        }

        refill();

        while (true) {
            int current = availableTokens.get();

            if (current < tokens) {
                if (listener != null) {
                    listener.onReject(tokens, current);
                }
                return false;
            }

            if (availableTokens.compareAndSet(current, current - tokens)) {
                if (listener != null) {
                    listener.onAcquire(tokens);
                }
                return true;
            }
        }
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    @Override
    public boolean tryAcquire(int tokens, long timeout, TimeUnit unit) throws InterruptedException {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens must be > 0, got: " + tokens);
        }

        long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
        long sleepIntervalMs = Math.max(1, unit.toMillis(timeout) / 20);

        while (System.nanoTime() < deadlineNanos) {
            if (tryAcquire(tokens)) {
                return true;
            }

            long remainingMs = (deadlineNanos - System.nanoTime()) / 1_000_000;
            if (remainingMs <= 0) {
                break;
            }

            Thread.sleep(Math.min(sleepIntervalMs, remainingMs));
        }

        return tryAcquire(tokens);
    }

    @Override
    public RateLimitResult tryAcquireWithInfo() {
        return tryAcquireWithInfo(1);
    }

    @Override
    public RateLimitResult tryAcquireWithInfo(int tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens must be > 0, got: " + tokens);
        }

        refill();

        while (true) {
            int current = availableTokens.get();

            if (current < tokens) {
                long retryAfterNanos = calculateRetryAfter(tokens - current);
                if (listener != null) {
                    listener.onReject(tokens, current);
                }
                return RateLimitResult.rejected(tokens, current, retryAfterNanos);
            }

            if (availableTokens.compareAndSet(current, current - tokens)) {
                if (listener != null) {
                    listener.onAcquire(tokens);
                }
                return RateLimitResult.success(tokens, current - tokens);
            }
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
        if (listener != null) {
            listener.onReset();
        }
    }

    /**
     * Get bucket capacity.
     * 
     * @return maximum tokens
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Refill tokens based on elapsed time.
     */
    private void refill() {
        if (refillPeriodNanos == 0 || refillTokens == 0) {
            return;
        }

        long now = System.nanoTime();
        long lastRefill = lastRefillTime.get();
        long timeSinceLastRefill = now - lastRefill;

        if (timeSinceLastRefill >= refillPeriodNanos) {
            long periodsElapsed = timeSinceLastRefill / refillPeriodNanos;
            int tokensToAdd = (int) Math.min(periodsElapsed * refillTokens, capacity);

            if (tokensToAdd > 0) {
                long newRefillTime = lastRefill + (periodsElapsed * refillPeriodNanos);

                if (lastRefillTime.compareAndSet(lastRefill, newRefillTime)) {
                    int newTotal = availableTokens.updateAndGet(current -> Math.min(capacity, current + tokensToAdd));
                    if (listener != null) {
                        listener.onRefill(tokensToAdd, newTotal);
                    }
                }
            }
        }
    }

    /**
     * Calculate estimated time until enough tokens are available.
     */
    private long calculateRetryAfter(int tokensNeeded) {
        if (refillPeriodNanos == 0 || refillTokens == 0) {
            return Long.MAX_VALUE;
        }
        long periodsNeeded = (tokensNeeded + refillTokens - 1) / refillTokens;
        return periodsNeeded * refillPeriodNanos;
    }
}
