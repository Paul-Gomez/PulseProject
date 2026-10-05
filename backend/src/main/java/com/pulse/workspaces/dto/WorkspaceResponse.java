package com.pulse.workspaces.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        String iconUrl,
        UUID ownerId,
        Instant createdAt,
        Instant updatedAt
) {
}
