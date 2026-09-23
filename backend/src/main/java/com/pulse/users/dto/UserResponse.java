package com.pulse.users.dto;

import com.pulse.users.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String displayName,
        String avatarUrl,
        UserStatus status,
        boolean emailVerified,
        Instant lastSeenAt,
        Instant createdAt
) {
}
