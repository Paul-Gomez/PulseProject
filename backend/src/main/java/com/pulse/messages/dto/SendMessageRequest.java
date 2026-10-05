package com.pulse.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        UUID parentMessageId,
        @Size(max = 5) List<UUID> attachmentIds
) {
    public SendMessageRequest(String content, UUID parentMessageId) {
        this(content, parentMessageId, null);
    }
}
