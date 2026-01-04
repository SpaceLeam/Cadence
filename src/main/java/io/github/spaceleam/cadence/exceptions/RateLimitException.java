package io.github.spaceleam.cadence.exceptions;

/**
 * Exception yang di-throw ketika request kena rate limit.
 */
public class RateLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RateLimitException() {
        super("Rate limit exceeded");
    }

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
