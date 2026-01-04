package io.github.spaceleam.cadence.core;

/**
 * Listener interface for rate limiter events.
 * Implement this to add monitoring, metrics, or logging.
 * 
 * <p>
 * Example with Micrometer:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     RateLimiter limiter = Cadence.builder()
 *             .capacity(100)
 *             .listener(new RateLimiterListener() {
 *                 Counter acquired = meterRegistry.counter("rate_limiter.acquired");
 *                 Counter rejected = meterRegistry.counter("rate_limiter.rejected");
 * 
 *                 &#64;Override
 *                 public void onAcquire(int tokens) {
 *                     acquired.increment(tokens);
 *                 }
 * 
 *                 @Override
 *                 public void onReject(int requested, int available) {
 *                     rejected.increment();
 *                 }
 *             })
 *             .build();
 * }
 * </pre>
 * 
 * @since 1.0.0
 */
public interface RateLimiterListener {

    /**
     * Called when tokens are successfully acquired.
     * 
     * @param tokensConsumed number of tokens consumed
     */
    void onAcquire(int tokensConsumed);

    /**
     * Called when a request is rejected due to insufficient tokens.
     * 
     * @param tokensRequested number of tokens that were requested
     * @param tokensAvailable number of tokens that were available
     */
    void onReject(int tokensRequested, int tokensAvailable);

    /**
     * Called when the rate limiter is reset.
     */
    default void onReset() {
        // Optional: default no-op
    }

    /**
     * Called when tokens are refilled.
     * 
     * @param tokensAdded number of tokens added
     * @param newTotal    new total available tokens
     */
    default void onRefill(int tokensAdded, int newTotal) {
        // Optional: default no-op
    }
}
