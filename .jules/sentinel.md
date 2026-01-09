# Sentinel's Journal

## 2026-01-09 - Integer Overflow in Token Refill
**Vulnerability:** The `refill()` method in `TokenBucketLimiter` calculated `tokensToAdd` using `periodsElapsed * refillTokens`. With `periodsElapsed` being a `long` and `refillTokens` an `int`, this multiplication could overflow `long` if the elapsed time was sufficiently large (e.g., 100 years, or just 5 seconds with extreme configuration). The result would wrap around to negative, causing `Math.min(result, capacity)` to be negative, and the `if (tokensToAdd > 0)` check to fail. This would effectively stop the bucket from refilling (Denial of Service).
**Learning:** Even with `long`, multiplication of time intervals by rates can overflow. `Math.min` does not protect against overflow that happens *before* the call. Explicit checks for overflow or clamping based on division are safer than multiplication.
**Prevention:** When calculating `a * b` where the result is capped at `C`, verify `a > C / b` before multiplying. If true, set result to `C` (or max value) directly.
