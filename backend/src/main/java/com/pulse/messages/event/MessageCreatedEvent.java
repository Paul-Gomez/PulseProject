package com.pulse.messages.event;

import com.pulse.messages.dto.MessageResponse;

public record MessageCreatedEvent(MessageResponse message) {
}
