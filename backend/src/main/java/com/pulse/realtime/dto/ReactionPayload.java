package com.pulse.realtime.dto;

import java.util.UUID;

public record ReactionPayload(UUID messageId, UUID userId, String emoji) {
}
