package com.pulse.workspaces.event;

import java.util.UUID;

public record MemberBannedEvent(UUID workspaceId, String workspaceName, UUID actorId, UUID targetUserId, String reason) {
}
