package com.pulse.notifications.dto;

import com.pulse.notifications.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID actorId,
        UUID workspaceId,
        UUID channelId,
        UUID messageId,
        UUID conversationId,
        String text,
        Instant readAt,
        Instant createdAt
) {
}
