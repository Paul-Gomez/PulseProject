package com.pulse.conversations.event;

import com.pulse.conversations.dto.DirectMessageResponse;

import java.util.List;
import java.util.UUID;

public record DirectMessageCreatedEvent(DirectMessageResponse message, List<UUID> memberIds) {
}
