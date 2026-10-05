package com.pulse.realtime.dto;

public record RealtimeEvent<T>(RealtimeEventType type, T payload) {
}
