package com.pulse.audit.listener;

import com.pulse.audit.entity.AuditAction;
import com.pulse.audit.service.AuditService;
import com.pulse.channels.event.ChannelArchivedEvent;
import com.pulse.messages.event.MessageModeratedEvent;
import com.pulse.workspaces.event.MemberBannedEvent;
import com.pulse.workspaces.event.MemberInvitedEvent;
import com.pulse.workspaces.event.MemberRemovedEvent;
import com.pulse.workspaces.event.MemberRoleChangedEvent;
import com.pulse.workspaces.event.MemberUnbannedEvent;
import com.pulse.workspaces.event.WorkspaceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes the audit trail from domain events, inside the same transaction as the action itself:
 * either both are saved or neither. Entries only carry ids and short facts, never message content.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final String USER = "USER";

    private final AuditService auditService;

    @EventListener
    public void onInvited(MemberInvitedEvent event) {
        auditService.record(event.workspaceId(), event.invitedById(), AuditAction.MEMBER_INVITED,
                USER, event.invitedUserId(), null);
    }

    @EventListener
    public void onRoleChanged(MemberRoleChangedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.MEMBER_ROLE_CHANGED,
                USER, event.targetUserId(), event.oldRole() + " -> " + event.newRole());
    }

    @EventListener
    public void onRemoved(MemberRemovedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.MEMBER_REMOVED,
                USER, event.targetUserId(), null);
    }

    @EventListener
    public void onBanned(MemberBannedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.MEMBER_BANNED,
                USER, event.targetUserId(), event.reason());
    }

    @EventListener
    public void onUnbanned(MemberUnbannedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.MEMBER_UNBANNED,
                USER, event.targetUserId(), null);
    }

    @EventListener
    public void onChannelArchived(ChannelArchivedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.CHANNEL_ARCHIVED,
                "CHANNEL", event.channelId(), event.channelName());
    }

    @EventListener
    public void onWorkspaceUpdated(WorkspaceUpdatedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.WORKSPACE_UPDATED,
                "WORKSPACE", event.workspaceId(), "changed: " + event.changedFields());
    }

    @EventListener
    public void onMessageModerated(MessageModeratedEvent event) {
        auditService.record(event.workspaceId(), event.actorId(), AuditAction.MESSAGE_DELETED_BY_MODERATOR,
                "MESSAGE", event.messageId(), "author: " + event.authorId() + ", channel: " + event.channelId());
    }
}
