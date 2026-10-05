package com.pulse.messages.event;

import java.util.UUID;

public record MessageModeratedEvent(UUID workspaceId, UUID channelId, UUID messageId, UUID actorId, UUID authorId) {
}
