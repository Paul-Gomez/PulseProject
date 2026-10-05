package com.pulse.workspaces.event;

import java.util.UUID;

public record WorkspaceUpdatedEvent(UUID workspaceId, UUID actorId, String changedFields) {
}
