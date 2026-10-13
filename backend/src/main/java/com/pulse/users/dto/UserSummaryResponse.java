package com.pulse.users.dto;

import com.pulse.users.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** What other users can see about someone: no email or account details. */
public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        UserStatus status,
        Instant lastSeenAt
) {
}
