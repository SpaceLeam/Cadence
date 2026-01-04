# Cadence

Lightweight in-memory rate limiter for JVM applications using Token Bucket algorithm.

[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Features

- **Zero dependencies** - Pure Java implementation, no external libraries required
- **Thread-safe** - Lock-free implementation using atomic operations
- **Low latency** - Sub-millisecond overhead per request
- **Flexible configuration** - Customizable capacity, refill rate, and weighted tokens
- **Fail-fast strategy** - Immediate rejection when limit exceeded

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.github.spaceleam:cadence:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.spaceleam</groupId>
    <artifactId>cadence</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```java
import io.github.spaceleam.cadence.Cadence;
import io.github.spaceleam.cadence.core.RateLimiter;

// Create a rate limiter: 100 requests per minute
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .refillRate(100, TimeUnit.MINUTES)
    .build();

// Check if request is allowed
if (limiter.tryAcquire()) {
    // Process request
} else {
    // Rate limit exceeded
}
```

## Usage Examples

### Per-User Rate Limiting

```java
Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();

public void handleRequest(String userId) {
    RateLimiter limiter = userLimiters.computeIfAbsent(userId, 
        id -> Cadence.builder()
            .capacity(10)
            .refillRate(10, TimeUnit.MINUTES)
            .build()
    );
    
    if (!limiter.tryAcquire()) {
        throw new RateLimitException("Rate limit exceeded for user: " + userId);
    }
}
```

### Weighted Token Consumption

```java
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .refillRate(50, TimeUnit.SECONDS)
    .build();

// Light request - consumes 1 token
limiter.tryAcquire(1);

// Heavy request - consumes 5 tokens
limiter.tryAcquire(5);
```

## Configuration Reference

| Use Case | Configuration |
|----------|---------------|
| Login attempts | `.capacity(5).refillRate(5, TimeUnit.MINUTES)` |
| OTP generation | `.capacity(3).refillRate(3, TimeUnit.HOURS)` |
| API calls | `.capacity(100).refillRate(100, TimeUnit.SECONDS)` |
| File downloads | `.capacity(10).refillRate(10, TimeUnit.DAYS)` |

## How It Works

Cadence implements the Token Bucket algorithm:

1. A bucket holds tokens up to a maximum capacity
2. Each request consumes one or more tokens
3. Tokens are replenished at a configured rate
4. Requests are rejected when the bucket is empty

```
Bucket capacity: 10 tokens
Refill rate: 2 tokens/second

t=0.0s: [●●●●●●●●●●] 10 tokens available
t=0.1s: [●●●●●●●●●○]  9 tokens (1 consumed)
t=0.2s: [●●●●●●●●○○]  8 tokens (1 consumed)
...
t=1.0s: [●●●●●●●●●●] 10 tokens (refilled)
```

## When to Use

**Recommended for:**
- Single-instance applications
- In-process rate limiting
- Low-latency requirements
- Database query throttling

**Consider alternatives for:**
- Distributed systems (use Redis-based solutions)
- IP-based rate limiting (use Nginx or Cloudflare)
- API Gateway environments (use built-in rate limiting)

## API Reference

### RateLimiter

| Method | Description |
|--------|-------------|
| `tryAcquire()` | Attempts to acquire 1 token. Returns `true` if successful. |
| `tryAcquire(int tokens)` | Attempts to acquire multiple tokens. Returns `true` if successful. |
| `getAvailableTokens()` | Returns the current number of available tokens. |
| `reset()` | Resets the bucket to full capacity. |

### CadenceBuilder

| Method | Description |
|--------|-------------|
| `capacity(int)` | Sets maximum token capacity. Default: 10 |
| `refillRate(int, TimeUnit)` | Sets tokens added per time unit. Default: 10/second |
| `build()` | Creates the RateLimiter instance. |

## Building from Source

```bash
git clone https://github.com/SpaceLeam/cadence.git
cd cadence
./gradlew build
```

## Requirements

- Java 21 or higher

## License

MIT License - see [LICENSE](LICENSE) for details.
