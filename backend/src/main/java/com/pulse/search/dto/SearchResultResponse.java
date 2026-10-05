package com.pulse.search.dto;

import java.time.Instant;
import java.util.UUID;

public record SearchResultResponse(
        UUID messageId,
        UUID channelId,
        String channelName,
        UUID workspaceId,
        UUID authorId,
        String authorDisplayName,
        String content,
        Instant createdAt
) {
}
