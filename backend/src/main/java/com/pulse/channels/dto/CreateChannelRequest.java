package com.pulse.channels.dto;

import com.pulse.channels.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChannelRequest(
        @NotBlank @Size(min = 1, max = 80) String name,
        @NotNull ChannelType type,
        boolean isPrivate
) {
}
