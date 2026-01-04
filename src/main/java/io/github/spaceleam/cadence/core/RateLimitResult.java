package io.github.spaceleam.cadence.core;

import java.util.concurrent.TimeUnit;

/**
 * Result of a rate limit check with detailed information.
 * Use this for debugging, logging, or returning retry information to clients.
 * 
 * @since 1.0.0
 */
public final class RateLimitResult {

    private final boolean success;
    private final int tokensRequested;
    private final int tokensAvailable;
    private final long retryAfterNanos;
    private final String reason;

    private RateLimitResult(boolean success, int tokensRequested, int tokensAvailable,
            long retryAfterNanos, String reason) {
        this.success = success;
        this.tokensRequested = tokensRequested;
        this.tokensAvailable = tokensAvailable;
        this.retryAfterNanos = retryAfterNanos;
        this.reason = reason;
    }

    /**
     * Create a successful result.
     */
    public static RateLimitResult success(int tokensConsumed, int tokensRemaining) {
        return new RateLimitResult(true, tokensConsumed, tokensRemaining, 0, null);
    }

    /**
     * Create a rejected result.
     */
    public static RateLimitResult rejected(int tokensRequested, int tokensAvailable,
            long retryAfterNanos) {
        String reason = String.format("Insufficient tokens: requested %d, available %d",
                tokensRequested, tokensAvailable);
        return new RateLimitResult(false, tokensRequested, tokensAvailable, retryAfterNanos, reason);
    }

    /**
     * Whether the request was allowed.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Number of tokens that were requested.
     */
    public int getTokensRequested() {
        return tokensRequested;
    }

    /**
     * Number of tokens available at time of check.
     */
    public int getTokensAvailable() {
        return tokensAvailable;
    }

    /**
     * Estimated time until enough tokens are available (in nanoseconds).
     * Returns 0 if the request was successful.
     */
    public long getRetryAfterNanos() {
        return retryAfterNanos;
    }

    /**
     * Get retry time in specified unit.
     */
    public long getRetryAfter(TimeUnit unit) {
        return unit.convert(retryAfterNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Human-readable reason for rejection.
     * Returns null if the request was successful.
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        if (success) {
            return "RateLimitResult{SUCCESS, consumed=" + tokensRequested +
                    ", remaining=" + tokensAvailable + "}";
        } else {
            return "RateLimitResult{REJECTED, " + reason +
                    ", retryAfter=" + getRetryAfter(TimeUnit.MILLISECONDS) + "ms}";
        }
    }
}
