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

    private static final String CHANNEL_EDIT = "CHANNEL_EDIT";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelAccessService channelAccessService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    /** Only people already inside the channel, or with CHANNEL_EDIT, can bring someone else in. */
    @Transactional
    public void addMember(UUID channelId, UUID requesterId, UUID targetUserId) {
        Channel channel = findChannelOrThrow(channelId);
        requireCanManage(channel, requesterId);
        workspaceAuthorizationService.requireMembership(channel.getWorkspaceId(), targetUserId);

        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, targetUserId)) {
            return;
        }
        channelMemberRepository.save(ChannelMember.builder()
                .channelId(channelId)
                .userId(targetUserId)
                .build());
    }

    /** Anyone can leave a channel; removing someone else needs CHANNEL_EDIT. */
    @Transactional
    public void removeMember(UUID channelId, UUID requesterId, UUID targetUserId) {
        Channel channel = findChannelOrThrow(channelId);
        if (!requesterId.equals(targetUserId)
                && !workspaceAuthorizationService.hasPermission(channel.getWorkspaceId(), requesterId, CHANNEL_EDIT)) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "You cannot remove other members from this channel");
        }
        channelMemberRepository.deleteByChannelIdAndUserId(channelId, targetUserId);
    }

    private void requireCanManage(Channel channel, UUID requesterId) {
        boolean canEdit = workspaceAuthorizationService.hasPermission(channel.getWorkspaceId(), requesterId, CHANNEL_EDIT);
        if (!canEdit && !channelAccessService.canAccess(channel, requesterId)) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "You cannot add members to this channel");
        }
    }

    private Channel findChannelOrThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Channel not found"));
    }
}
