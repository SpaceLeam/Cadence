# Usage Guide

Comprehensive guide for using Cadence rate limiter in your applications.

## Table of Contents

- [Basic Usage](#basic-usage)
- [Preset Configurations](#preset-configurations)
- [Per-User Rate Limiting](#per-user-rate-limiting)
- [Weighted Tokens](#weighted-tokens)
- [Timeout with Waiting](#timeout-with-waiting)
- [Monitoring with Listeners](#monitoring-with-listeners)
- [Detailed Result Information](#detailed-result-information)
- [Spring Boot Integration](#spring-boot-integration)
- [Performance Tuning](#performance-tuning)

---

## Basic Usage

```java
import io.github.spaceleam.cadence.Cadence;
import io.github.spaceleam.cadence.core.RateLimiter;

// Create a rate limiter: 100 requests per minute
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .refillRate(100, TimeUnit.MINUTES)
    .build();

// In your code
public Response handleRequest() {
    if (!limiter.tryAcquire()) {
        return Response.status(429).entity("Too many requests").build();
    }
    return processRequest();
}
```

---

## Preset Configurations

Use built-in presets for common scenarios:

```java
// Login protection: 5 attempts per minute
RateLimiter loginLimiter = Cadence.forLogin();

// OTP/SMS: 3 requests per hour
RateLimiter otpLimiter = Cadence.forOTP();

// Standard API: 100 requests per second
RateLimiter apiLimiter = Cadence.forAPI();

// File downloads: 10 per hour
RateLimiter downloadLimiter = Cadence.forDownload();

// Search/heavy queries: 30 per minute
RateLimiter searchLimiter = Cadence.forSearch();
```

---

## Per-User Rate Limiting

Each user gets their own rate limit:

```java
private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();

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
    // Process request
}
```

---

## Weighted Tokens

Different operations consume different amounts:

```java
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .refillRate(50, TimeUnit.SECONDS)
    .build();

// Light operation: 1 token
public void ping() {
    limiter.tryAcquire(1);
}

// Medium operation: 5 tokens
public void search(String query) {
    limiter.tryAcquire(5);
}

// Heavy operation: 20 tokens
public void exportReport() {
    limiter.tryAcquire(20);
}
```

---

## Timeout with Waiting

Wait for a token instead of immediate rejection:

```java
RateLimiter limiter = Cadence.builder()
    .capacity(10)
    .refillRate(10, TimeUnit.SECONDS)
    .build();

try {
    // Wait up to 5 seconds for a token
    if (limiter.tryAcquire(5, TimeUnit.SECONDS)) {
        processRequest();
    } else {
        return "Request timed out - system too busy";
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return "Request cancelled";
}
```

---

## Monitoring with Listeners

Integrate with your metrics system:

```java
RateLimiter limiter = Cadence.builder()
    .capacity(100)
    .listener(new RateLimiterListener() {
        @Override
        public void onAcquire(int tokens) {
            metrics.counter("rate_limiter.acquired").increment(tokens);
        }
        
        @Override
        public void onReject(int requested, int available) {
            metrics.counter("rate_limiter.rejected").increment();
            log.warn("Rate limited: requested={}, available={}", requested, available);
        }
        
        @Override
        public void onReset() {
            log.info("Rate limiter reset");
        }
    })
    .build();
```

---

## Detailed Result Information

Get retry information for clients:

```java
RateLimitResult result = limiter.tryAcquireWithInfo();

if (result.isSuccess()) {
    processRequest();
} else {
    // Return retry-after header
    long retryAfterSeconds = result.getRetryAfter(TimeUnit.SECONDS);
    
    return Response.status(429)
        .header("Retry-After", retryAfterSeconds)
        .entity(result.getReason())
        .build();
}
```

---

## Spring Boot Integration

### Basic Interceptor

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        String clientId = getClientId(request);
        
        RateLimiter limiter = limiters.computeIfAbsent(clientId, 
            id -> Cadence.forAPI());
        
        if (!limiter.tryAcquire()) {
            response.setStatus(429);
            response.getWriter().write("Too many requests");
            return false;
        }
        return true;
    }
    
    private String getClientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded : request.getRemoteAddr();
    }
}
```

### Annotation-Based

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int capacity() default 100;
    int refillRate() default 100;
    TimeUnit timeUnit() default TimeUnit.MINUTES;
}

// Usage
@RateLimit(capacity = 5, refillRate = 5, timeUnit = TimeUnit.MINUTES)
@PostMapping("/login")
public LoginResponse login(@RequestBody LoginRequest request) {
    // ...
}
```

---

## Performance Tuning

### Memory Optimization

Each limiter uses ~100 bytes. For millions of users:

```java
// Use weak references for auto-cleanup
MapMaker mapMaker = new MapMaker().weakValues();
Map<String, RateLimiter> limiters = mapMaker.makeMap();
```

### High Throughput

For maximum performance:

```java
RateLimiter limiter = Cadence.builder()
    .capacity(Integer.MAX_VALUE)  // Effectively unlimited burst
    .refillRate(1000, TimeUnit.SECONDS)
    .build();
```

### Avoid Contention

For very high concurrency, use multiple limiters:

```java
// Striped limiters to reduce contention
private final RateLimiter[] limiters = new RateLimiter[16];

public RateLimiter getLimiter(String key) {
    int index = Math.abs(key.hashCode() % limiters.length);
    return limiters[index];
}
```

---

## Common Patterns

### Global + Per-User Limiting

```java
RateLimiter globalLimiter = Cadence.builder()
    .capacity(10000)
    .refillRate(10000, TimeUnit.SECONDS)
    .build();

Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();

public boolean checkRateLimit(String userId) {
    // Global limit first
    if (!globalLimiter.tryAcquire()) {
        return false;
    }
    
    // Then per-user limit
    return userLimiters
        .computeIfAbsent(userId, id -> Cadence.forAPI())
        .tryAcquire();
}
```

### Graceful Degradation

```java
public Response handleRequest() {
    if (!primaryLimiter.tryAcquire()) {
        // Try fallback with longer wait
        if (!fallbackLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            return cachedResponse(); // Return cached data
        }
    }
    return liveResponse();
}
```
