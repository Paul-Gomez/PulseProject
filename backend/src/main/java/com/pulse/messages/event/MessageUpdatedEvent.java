package com.pulse.messages.event;

import com.pulse.messages.dto.MessageResponse;

public record MessageUpdatedEvent(MessageResponse message) {
}
