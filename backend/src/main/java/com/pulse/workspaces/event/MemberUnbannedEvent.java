package com.pulse.workspaces.event;

import java.util.UUID;

public record MemberUnbannedEvent(UUID workspaceId, UUID actorId, UUID targetUserId) {
}
