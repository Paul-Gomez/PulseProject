package com.pulse.conversations.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        boolean isGroup,
        String name,
        List<UUID> memberIds,
        long unreadCount,
        Instant createdAt,
        Instant lastActivityAt
) {
}
