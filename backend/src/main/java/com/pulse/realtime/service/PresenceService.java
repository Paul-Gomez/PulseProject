package com.pulse.realtime.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks open WebSocket sessions per user. A user can have several (web + mobile, several tabs),
 * so they are only "offline" when the last session closes.
 */
@Service
public class PresenceService {

    private final Map<UUID, Set<String>> sessionsByUser = new ConcurrentHashMap<>();

    /** @return true if this was the user's first open session (they just came online) */
    public boolean registerSession(UUID userId, String sessionId) {
        AtomicBoolean firstSession = new AtomicBoolean(false);
        sessionsByUser.compute(userId, (id, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
                firstSession.set(true);
            }
            sessions.add(sessionId);
            return sessions;
        });
        return firstSession.get();
    }

    /** @return true if that was the user's last open session (they just went offline) */
    public boolean unregisterSession(UUID userId, String sessionId) {
        AtomicBoolean lastSession = new AtomicBoolean(false);
        sessionsByUser.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                lastSession.set(true);
                return null;
            }
            return sessions;
        });
        return lastSession.get();
    }

    public boolean isOnline(UUID userId) {
        return sessionsByUser.containsKey(userId);
    }
}
