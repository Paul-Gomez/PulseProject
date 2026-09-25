package com.pulse.messages.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID channelId,
        UUID authorId,
        String content,
        UUID parentMessageId,
        Instant editedAt,
        Instant deletedAt,
        Instant createdAt
) {
}
