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

    public void requireAccess(Channel channel, UUID userId) {
        if (channel.isPrivate()) {
            boolean isChannelMember = channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), userId);
            if (!isChannelMember) {
                throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "Not a member of this private channel");
            }
            return;
        }

        // Public channels are visible to any member of the parent workspace.
        workspaceAuthorizationService.requireMembership(channel.getWorkspaceId(), userId);
    }
}
