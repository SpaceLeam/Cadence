package io.github.spaceleam.cadence.core;

import java.util.concurrent.TimeUnit;

/**
 * Core interface for rate limiting.
 * All rate limiter implementations must implement this interface.
 * 
 * @since 1.0.0
 */
public interface RateLimiter {

    /**
     * Attempt to acquire 1 token.
     * 
     * @return true if token was acquired, false if rate limited
     */
    boolean tryAcquire();

    /**
     * Attempt to acquire multiple tokens (weighted request).
     * 
     * @param tokens number of tokens to acquire
     * @return true if tokens were acquired, false if insufficient
     * @throws IllegalArgumentException if tokens is not positive
     */
    boolean tryAcquire(int tokens);

    /**
     * Attempt to acquire 1 token with timeout.
     * Blocks until token is available or timeout expires.
     * 
     * @param timeout maximum time to wait
     * @param unit    time unit of the timeout
     * @return true if token was acquired, false if timeout expired
     * @throws InterruptedException if thread is interrupted while waiting
     */
    boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Attempt to acquire tokens with timeout.
     * 
     * @param tokens  number of tokens to acquire
     * @param timeout maximum time to wait
     * @param unit    time unit of the timeout
     * @return true if tokens were acquired, false if timeout expired
     * @throws InterruptedException if thread is interrupted while waiting
     */
    boolean tryAcquire(int tokens, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Attempt to acquire with detailed result information.
     * Use this when you need retry information or debugging.
     * 
     * @return result with success status and details
     */
    RateLimitResult tryAcquireWithInfo();

    /**
     * Attempt to acquire multiple tokens with detailed result.
     * 
     * @param tokens number of tokens to acquire
     * @return result with success status and details
     */
    RateLimitResult tryAcquireWithInfo(int tokens);

    /**
     * Get current available tokens.
     * 
     * @return number of available tokens
     */
    int getAvailableTokens();

    /**
     * Reset bucket to full capacity.
     */
    void reset();
}
