package com.pulse.channels.listener;

import com.pulse.channels.repository.ChannelMemberRepository;
import com.pulse.workspaces.event.MemberBannedEvent;
import com.pulse.workspaces.event.MemberRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** When someone leaves a workspace they also leave its channels, so being re-invited does not bring old access back. */
@Component
@RequiredArgsConstructor
public class WorkspaceMembershipListener {

    private final ChannelMemberRepository channelMemberRepository;

    @EventListener
    public void onMemberRemoved(MemberRemovedEvent event) {
        channelMemberRepository.deleteAllForUserInWorkspace(event.targetUserId(), event.workspaceId());
    }

    @EventListener
    public void onMemberBanned(MemberBannedEvent event) {
        channelMemberRepository.deleteAllForUserInWorkspace(event.targetUserId(), event.workspaceId());
    }
}
