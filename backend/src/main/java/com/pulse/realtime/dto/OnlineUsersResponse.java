package com.pulse.realtime.dto;

import java.util.List;
import java.util.UUID;

public record OnlineUsersResponse(List<UUID> onlineUserIds) {
}
