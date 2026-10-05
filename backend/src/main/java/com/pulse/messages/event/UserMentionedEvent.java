package com.pulse.messages.event;

import java.util.UUID;

public record UserMentionedEvent(UUID workspaceId, UUID channelId, UUID messageId, UUID authorId,
                                 UUID mentionedUserId, String preview) {
}
