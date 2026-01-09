## 2024-05-22 - Arithmetic Overflow in Token Bucket
**Learning:** Multiplying two large numbers (periods * rate) to calculate refill amount can overflow `long` even if the result is clamped to a small `int` capacity later. Always clamp inputs *before* multiplication if possible, or use saturation arithmetic.
**Action:** When calculating `periods * rate`, check if `periods > capacity` first. If so, skip the multiplication and use `capacity`. This optimizes performance (skip math) and ensures safety (avoid overflow).
