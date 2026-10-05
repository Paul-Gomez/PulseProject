package com.pulse.realtime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Presence lives in Redis, not in Postgres: it changes constantly and is worthless after a few minutes.
 *
 * Each user has a sorted set (member = websocket session id, score = the moment that session expires).
 * The instance that owns a session keeps renewing its score while it is connected, so if an instance
 * crashes its sessions simply run out and the user goes offline by themselves. A user is online while
 * the set holds at least one non-expired session (web + mobile, several tabs...).
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Duration SESSION_TTL = Duration.ofSeconds(90);
    private static final String KEY_PREFIX = "presence:user:";

    // Both scripts are atomic, so two sessions connecting at the same time cannot both think they were the first.
    private static final DefaultRedisScript<Long> REGISTER_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            redis.call('ZADD', KEYS[1], ARGV[2], ARGV[3])
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return redis.call('ZCARD', KEYS[1])
            """, Long.class);

    private static final DefaultRedisScript<Long> UNREGISTER_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZREM', KEYS[1], ARGV[2])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            return redis.call('ZCARD', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Map<String, UUID> localSessions = new ConcurrentHashMap<>();

    /** @return true if this was the user's first live session (they just came online) */
    public boolean registerSession(UUID userId, String sessionId) {
        long now = System.currentTimeMillis();
        Long sessions = redis.execute(REGISTER_SCRIPT, List.of(key(userId)),
                String.valueOf(now),
                String.valueOf(now + SESSION_TTL.toMillis()),
                sessionId,
                String.valueOf(SESSION_TTL.toMillis() * 3));
        localSessions.put(sessionId, userId);
        return sessions != null && sessions == 1;
    }

    /** @return true if that was the user's last live session (they just went offline) */
    public boolean unregisterSession(UUID userId, String sessionId) {
        localSessions.remove(sessionId);
        Long remaining = redis.execute(UNREGISTER_SCRIPT, List.of(key(userId)),
                String.valueOf(System.currentTimeMillis()), sessionId);
        return remaining != null && remaining == 0;
    }

    public boolean isOnline(UUID userId) {
        Long live = redis.opsForZSet().count(key(userId), System.currentTimeMillis(), Double.POSITIVE_INFINITY);
        return live != null && live > 0;
    }

    @Scheduled(fixedDelayString = "${pulse.presence.refresh-interval-ms:30000}")
    public void renewLocalSessions() {
        long expiresAt = System.currentTimeMillis() + SESSION_TTL.toMillis();
        localSessions.forEach((sessionId, userId) -> {
            String key = key(userId);
            redis.opsForZSet().add(key, sessionId, expiresAt);
            redis.expire(key, SESSION_TTL.multipliedBy(3));
        });
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
