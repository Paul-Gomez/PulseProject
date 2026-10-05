package com.pulse.workspaces.event;

import java.util.UUID;

public record MemberRoleChangedEvent(UUID workspaceId, UUID actorId, UUID targetUserId, String oldRole, String newRole) {
}
