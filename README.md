# Cadence

Lightweight in-memory rate limiter for JVM applications using Token Bucket algorithm.

[![Build](https://img.shields.io/github/actions/workflow/status/SpaceLeam/cadence/build.yml?branch=main)](https://github.com/SpaceLeam/cadence/actions)
[![Coverage](https://img.shields.io/codecov/c/github/SpaceLeam/cadence)](https://codecov.io/gh/SpaceLeam/cadence)
[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Features

- **Zero dependencies** - Pure Java implementation
- **Thread-safe** - Lock-free atomic operations
- **Sub-millisecond latency** - Minimal overhead
- **Flexible** - Custom capacity, refill rate, weighted tokens
- **Timeout support** - Wait for tokens with configurable timeout
- **Monitoring hooks** - Integrate with metrics systems
- **Preset configs** - Ready-to-use configurations

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

// Create rate limiter: 100 requests per minute
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .refillRate(100, TimeUnit.MINUTES)
    .build();

if (limiter.tryAcquire()) {
    // Process request
} else {
    // Rate limited
}
```

## Preset Configurations

```java
RateLimiter loginLimiter = Cadence.forLogin();     // 5 req/min
RateLimiter apiLimiter = Cadence.forAPI();         // 100 req/sec
RateLimiter otpLimiter = Cadence.forOTP();         // 3 req/hour
RateLimiter downloadLimiter = Cadence.forDownload(); // 10 req/hour
```

## Advanced Features

### Timeout with Waiting

```java
// Wait up to 5 seconds for a token
if (limiter.tryAcquire(5, TimeUnit.SECONDS)) {
    processRequest();
}
```

### Detailed Result

```java
RateLimitResult result = limiter.tryAcquireWithInfo();
if (!result.isSuccess()) {
    long retryAfter = result.getRetryAfter(TimeUnit.SECONDS);
    return Response.status(429).header("Retry-After", retryAfter).build();
}
```

### Monitoring

```java
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .listener(new RateLimiterListener() {
        @Override
        public void onAcquire(int tokens) {
            metrics.increment("acquired", tokens);
        }
        @Override
        public void onReject(int requested, int available) {
            metrics.increment("rejected");
        }
    })
    .build();
```

## Documentation

- [USAGE.md](USAGE.md) - Comprehensive usage guide
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

## API Reference

### RateLimiter

| Method | Description |
|--------|-------------|
| `tryAcquire()` | Acquire 1 token |
| `tryAcquire(int tokens)` | Acquire multiple tokens |
| `tryAcquire(timeout, unit)` | Acquire with timeout |
| `tryAcquireWithInfo()` | Acquire with detailed result |
| `getAvailableTokens()` | Current available tokens |
| `reset()` | Reset to full capacity |

### CadenceBuilder

| Method | Description |
|--------|-------------|
| `capacity(int)` | Max tokens (default: 10) |
| `refillRate(int, TimeUnit)` | Tokens per time unit |
| `listener(RateLimiterListener)` | Add monitoring listener |

## Performance

| Metric | Value |
|--------|-------|
| Single-thread throughput | >1M ops/sec |
| Multi-thread throughput | >5M ops/sec |
| Latency (p50) | <1 µs |
| Memory per instance | ~100 bytes |

## Requirements

- Java 21+

## License

MIT License - see [LICENSE](LICENSE)
