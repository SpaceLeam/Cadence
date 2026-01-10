# Bolt's Journal

## 2024-05-22 - TokenBucketLimiter Concurrency
**Learning:** `AtomicInteger.updateAndGet` with capturing lambdas allocates a new object for the lambda on every call in hot paths.
**Action:** In high-throughput `tryAcquire` methods, replace `updateAndGet` with a manual `compareAndSet` loop to eliminate allocation overhead.

## 2024-05-22 - Integer Overflow in Refill Logic
**Learning:** Calculating `tokensToAdd` as `periods * refillTokens` can overflow `int` (and even `long` if not careful) before clamping to `capacity`.
**Action:** Always use `long` arithmetic for intermediate token calculations and clamp to `capacity` *before* casting back to the storage type or applying updates.
