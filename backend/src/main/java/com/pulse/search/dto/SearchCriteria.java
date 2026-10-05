package com.pulse.search.dto;

import java.time.Instant;
import java.util.UUID;

public record SearchCriteria(
        String text,
        UUID workspaceId,
        UUID channelId,
        UUID authorId,
        UUID mentionedUserId,
        Instant from,
        Instant to
) {
}
