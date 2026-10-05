package com.pulse.conversations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendDirectMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        UUID parentMessageId
) {
}
