package com.pulse.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Fixed-window counters in Redis. If Redis is unavailable the limiter lets requests through:
 * a Redis outage should not take login down with it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    // INCR + EXPIRE in one atomic script, so a crash in between can never leave a counter without expiry.
    private static final DefaultRedisScript<Long> HIT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;

    public boolean tryAcquire(String key, int limit, Duration window) {
        return hit(key, window) <= limit;
    }

    public long hit(String key, Duration window) {
        if (!properties.enabled()) {
            return 0;
        }
        try {
            Long count = redis.execute(HIT_SCRIPT, List.of(key), String.valueOf(window.toMillis()));
            return count == null ? 0 : count;
        } catch (RuntimeException ex) {
            log.warn("Rate limiter unavailable, letting the request through: {}", ex.getMessage());
            return 0;
        }
    }

    public long current(String key) {
        if (!properties.enabled()) {
            return 0;
        }
        try {
            String value = redis.opsForValue().get(key);
            return value == null ? 0 : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Rate limiter unavailable, ignoring counter: {}", ex.getMessage());
            return 0;
        }
    }

    public void reset(String key) {
        if (!properties.enabled()) {
            return;
        }
        try {
            redis.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Rate limiter unavailable, could not reset counter: {}", ex.getMessage());
        }
    }
}
