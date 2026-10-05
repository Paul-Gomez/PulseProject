package com.pulse.workspaces.event;

import java.util.UUID;

public record MemberRemovedEvent(UUID workspaceId, String workspaceName, UUID actorId, UUID targetUserId) {
}
