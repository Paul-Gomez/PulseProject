package com.pulse.notifications.listener;

import com.pulse.conversations.event.DirectMessageCreatedEvent;
import com.pulse.notifications.entity.NotificationType;
import com.pulse.notifications.service.NotificationService;
import com.pulse.notifications.service.NotificationService.Target;
import com.pulse.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DirectMessageNotificationListener {

    private static final int PREVIEW_LENGTH = 100;

    private final NotificationService notificationService;
    private final UserService userService;

    @EventListener
    public void onDirectMessage(DirectMessageCreatedEvent event) {
        UUID authorId = event.message().authorId();
        String author = userService.getById(authorId).displayName();
        String content = event.message().content();
        String preview = content.length() <= PREVIEW_LENGTH ? content : content.substring(0, PREVIEW_LENGTH) + "...";

        for (UUID memberId : event.memberIds()) {
            if (!memberId.equals(authorId)) {
                notificationService.create(memberId, NotificationType.DIRECT_MESSAGE, authorId,
                        new Target(null, null, event.message().id(), event.message().conversationId()),
                        author + ": " + preview);
            }
        }
    }
}
