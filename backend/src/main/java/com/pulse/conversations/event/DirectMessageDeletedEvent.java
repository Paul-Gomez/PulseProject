package com.pulse.conversations.event;

import java.util.UUID;

public record DirectMessageDeletedEvent(UUID conversationId, UUID messageId) {
}
