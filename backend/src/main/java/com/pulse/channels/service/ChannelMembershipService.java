package com.pulse.channels.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.entity.ChannelMember;
import com.pulse.channels.repository.ChannelMemberRepository;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelMembershipService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Transactional
    public void addMember(UUID channelId, UUID requesterId, UUID targetUserId) {
        Channel channel = findChannelOrThrow(channelId);
        workspaceAuthorizationService.requireMembership(channel.getWorkspaceId(), requesterId);
        workspaceAuthorizationService.requireMembership(channel.getWorkspaceId(), targetUserId);

        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, targetUserId)) {
            return;
        }

        channelMemberRepository.save(ChannelMember.builder()
                .channelId(channelId)
                .userId(targetUserId)
                .build());
    }

    @Transactional
    public void removeMember(UUID channelId, UUID requesterId, UUID targetUserId) {
        Channel channel = findChannelOrThrow(channelId);
        workspaceAuthorizationService.requireMembership(channel.getWorkspaceId(), requesterId);
        channelMemberRepository.deleteByChannelIdAndUserId(channelId, targetUserId);
    }

    private Channel findChannelOrThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Channel not found"));
    }
}
