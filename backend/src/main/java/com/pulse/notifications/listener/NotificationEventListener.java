package com.pulse.notifications.listener;

import com.pulse.messages.event.MessageModeratedEvent;
import com.pulse.messages.event.UserMentionedEvent;
import com.pulse.notifications.entity.NotificationType;
import com.pulse.notifications.service.NotificationService;
import com.pulse.notifications.service.NotificationService.Target;
import com.pulse.users.service.UserService;
import com.pulse.workspaces.event.MemberBannedEvent;
import com.pulse.workspaces.event.MemberInvitedEvent;
import com.pulse.workspaces.event.MemberRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns domain events from other modules into notifications. Runs inside the publisher's transaction,
 * so a notification only exists if the action that caused it was saved.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserService userService;

    @EventListener
    public void onMention(UserMentionedEvent event) {
        String author = nameOf(event.authorId());
        notificationService.create(event.mentionedUserId(), NotificationType.MENTION, event.authorId(),
                new Target(event.workspaceId(), event.channelId(), event.messageId(), null),
                author + " mentioned you: " + event.preview());
    }

    @EventListener
    public void onInvited(MemberInvitedEvent event) {
        notificationService.create(event.invitedUserId(), NotificationType.INVITATION, event.invitedById(),
                new Target(event.workspaceId(), null, null, null),
                nameOf(event.invitedById()) + " added you to " + event.workspaceName());
    }

    @EventListener
    public void onRemoved(MemberRemovedEvent event) {
        if (event.targetUserId().equals(event.actorId())) {
            return;
        }
        notificationService.create(event.targetUserId(), NotificationType.MODERATION, event.actorId(),
                new Target(event.workspaceId(), null, null, null),
                "You were removed from " + event.workspaceName());
    }

    @EventListener
    public void onBanned(MemberBannedEvent event) {
        String reason = event.reason() == null || event.reason().isBlank() ? "" : " Reason: " + event.reason();
        notificationService.create(event.targetUserId(), NotificationType.MODERATION, event.actorId(),
                new Target(event.workspaceId(), null, null, null),
                "You were banned from " + event.workspaceName() + "." + reason);
    }

    @EventListener
    public void onMessageModerated(MessageModeratedEvent event) {
        notificationService.create(event.authorId(), NotificationType.MODERATION, event.actorId(),
                new Target(event.workspaceId(), event.channelId(), event.messageId(), null),
                "A moderator deleted one of your messages");
    }

    private String nameOf(java.util.UUID userId) {
        return userService.getById(userId).displayName();
    }
}
