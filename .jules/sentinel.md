## 2024-05-23 - Integer Overflow in Rate Limiter Refill
**Vulnerability:** Integer overflow in token bucket refill logic allowed tokens to become negative or wrap around when capacity or time gaps were large.
**Learning:** `Math.min(int, long)` promotes to long, but subsequent casts or operations can still overflow if intermediate values exceed type limits.
**Prevention:** Always use `long` arithmetic for intermediate calculations involving time or accumulation, and clamp to limits *before* casting back to smaller types. Use preconditions to skip calculations that would obviously exceed limits.
