package com.pulse.realtime.dto;

import java.util.UUID;

public record TypingPayload(UUID channelId, UUID userId, String displayName) {
}
