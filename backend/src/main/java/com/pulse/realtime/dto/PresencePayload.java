package com.pulse.realtime.dto;

import com.pulse.users.entity.UserStatus;

import java.util.UUID;

public record PresencePayload(UUID userId, UserStatus status) {
}
