package com.pulse.workspaces.dto;

import java.time.Instant;
import java.util.UUID;

public record BanResponse(UUID userId, UUID bannedBy, String reason, Instant createdAt) {
}
