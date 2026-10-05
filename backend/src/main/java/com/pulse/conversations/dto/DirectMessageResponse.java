package com.pulse.conversations.dto;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageResponse(
        UUID id,
        UUID conversationId,
        UUID authorId,
        String content,
        UUID parentMessageId,
        Instant createdAt
) {
}
