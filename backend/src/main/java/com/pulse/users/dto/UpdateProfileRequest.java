package com.pulse.users.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100) String displayName,
        @Size(max = 500) String avatarUrl
) {
}
