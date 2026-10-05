package com.pulse.channels.dto;

import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
        @Size(min = 1, max = 80) String name,
        Integer position
) {
}
