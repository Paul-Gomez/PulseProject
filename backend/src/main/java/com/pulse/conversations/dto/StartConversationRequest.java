package com.pulse.conversations.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record StartConversationRequest(
        @NotEmpty @Size(max = 9) List<UUID> participantIds,
        @Size(max = 100) String name
) {
}
