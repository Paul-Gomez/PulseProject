package com.pulse.workspaces.event;

import java.util.UUID;

public record MemberInvitedEvent(UUID workspaceId, String workspaceName, UUID invitedUserId, UUID invitedById) {
}
