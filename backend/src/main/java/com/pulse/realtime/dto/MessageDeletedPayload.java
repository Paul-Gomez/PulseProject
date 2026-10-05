package com.pulse.realtime.dto;

import java.util.UUID;

public record MessageDeletedPayload(UUID channelId, UUID messageId) {
}
