package com.pulse.workspaces.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID userId,
        String roleName,
        Instant joinedAt
) {
}
