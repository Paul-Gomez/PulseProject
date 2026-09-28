package com.pulse.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(
        @NotBlank @Size(max = 20) String emoji
) {
}
