package com.pulse.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        UUID parentMessageId
) {
}
