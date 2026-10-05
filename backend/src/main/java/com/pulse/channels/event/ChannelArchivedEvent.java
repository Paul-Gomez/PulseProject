package com.pulse.channels.event;

import java.util.UUID;

public record ChannelArchivedEvent(UUID workspaceId, UUID actorId, UUID channelId, String channelName) {
}
