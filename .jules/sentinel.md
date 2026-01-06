## 2025-02-14 - Token Bucket Integer Overflow
**Vulnerability:** Integer overflow in `periodsElapsed * refillTokens` calculation in `TokenBucketLimiter` caused negative or incorrect token refill values when `refillTokens` was large and `timeSinceLastRefill` was significant.
**Learning:** Standard multiplication checks are insufficient when dealing with `long` time periods and `int` token counts. Overflow can wrap around to positive values, masking the error but producing incorrect results.
**Prevention:** Use `long` division `(a > MAX / b)` to check for overflow before multiplication, or cast to `BigInteger` for safety if performance allows (though manual check is faster). Always clamp refill amounts to capacity.
