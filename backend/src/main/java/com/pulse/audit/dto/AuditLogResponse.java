package com.pulse.audit.dto;

import com.pulse.audit.entity.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID workspaceId,
        UUID actorId,
        String actorUsername,
        AuditAction action,
        String resourceType,
        UUID resourceId,
        String details,
        Instant createdAt
) {
}
