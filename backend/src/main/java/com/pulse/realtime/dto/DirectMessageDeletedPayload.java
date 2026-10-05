package com.pulse.realtime.dto;

import java.util.UUID;

public record DirectMessageDeletedPayload(UUID conversationId, UUID messageId) {
}
