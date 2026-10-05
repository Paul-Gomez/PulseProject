package com.pulse.messages.event;

import java.util.UUID;

public record ReactionChangedEvent(UUID channelId, UUID messageId, UUID userId, String emoji, boolean added) {
}
