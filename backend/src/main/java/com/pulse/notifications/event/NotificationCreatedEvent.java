package com.pulse.notifications.event;

import com.pulse.notifications.dto.NotificationResponse;

import java.util.UUID;

public record NotificationCreatedEvent(UUID userId, NotificationResponse notification) {
}
