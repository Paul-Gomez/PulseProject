package com.pulse.channels.dto;

import com.pulse.channels.entity.ChannelType;

import java.time.Instant;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        UUID workspaceId,
        String name,
        ChannelType type,
        boolean isPrivate,
        UUID createdBy,
        int position,
        Instant archivedAt,
        Instant createdAt
) {
}
