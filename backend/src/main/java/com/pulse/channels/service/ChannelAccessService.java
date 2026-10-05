package com.pulse.channels.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelMemberRepository;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelAccessService {

    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    /**
     * Being in the workspace is always required; private channels additionally need explicit membership.
     * Checking the workspace first means someone removed from it can never read its private channels.
     */
    public boolean canAccess(Channel channel, UUID userId) {
        if (!workspaceAuthorizationService.isMember(channel.getWorkspaceId(), userId)) {
            return false;
        }
        return !channel.isPrivate() || channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), userId);
    }

    public void requireAccess(Channel channel, UUID userId) {
        if (!canAccess(channel, userId)) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "You do not have access to this channel");
        }
    }
}
