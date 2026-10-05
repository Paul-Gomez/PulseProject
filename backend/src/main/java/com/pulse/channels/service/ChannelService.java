package com.pulse.channels.service;

import com.pulse.channels.dto.ChannelResponse;
import com.pulse.channels.dto.CreateChannelRequest;
import com.pulse.channels.dto.UpdateChannelRequest;
import com.pulse.channels.entity.Channel;
import com.pulse.channels.entity.ChannelMember;
import com.pulse.channels.event.ChannelArchivedEvent;
import com.pulse.channels.mapper.ChannelMapper;
import com.pulse.channels.repository.ChannelMemberRepository;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelMapper channelMapper;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ChannelAccessService channelAccessService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChannelResponse create(UUID workspaceId, UUID requesterId, CreateChannelRequest request) {
        workspaceAuthorizationService.requirePermission(workspaceId, requesterId, "CHANNEL_CREATE");

        if (channelRepository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, request.name())) {
            throw ApiException.conflict(ErrorCode.CHANNEL_NAME_TAKEN, "A channel with that name already exists");
        }

        Channel channel = Channel.builder()
                .workspaceId(workspaceId)
                .name(request.name())
                .type(request.type())
                .isPrivate(request.isPrivate())
                .createdBy(requesterId)
                .build();

        Channel saved = channelRepository.save(channel);

        if (saved.isPrivate()) {
            channelMemberRepository.save(ChannelMember.builder()
                    .channelId(saved.getId())
                    .userId(requesterId)
                    .build());
        }

        return channelMapper.toResponse(saved);
    }

    public List<ChannelResponse> listForWorkspace(UUID workspaceId, UUID requesterId) {
        workspaceAuthorizationService.requireMembership(workspaceId, requesterId);
        return channelRepository.findAllByWorkspaceIdAndArchivedAtIsNullOrderByPositionAsc(workspaceId).stream()
                .map(channelMapper::toResponse)
                .toList();
    }

    public ChannelResponse getById(UUID channelId, UUID requesterId) {
        Channel channel = findChannelOrThrow(channelId);
        channelAccessService.requireAccess(channel, requesterId);
        return channelMapper.toResponse(channel);
    }

    @Transactional
    public ChannelResponse update(UUID channelId, UUID requesterId, UpdateChannelRequest request) {
        Channel channel = findChannelOrThrow(channelId);
        workspaceAuthorizationService.requirePermission(channel.getWorkspaceId(), requesterId, "CHANNEL_EDIT");

        if (request.name() != null) {
            channel.setName(request.name());
        }
        if (request.position() != null) {
            channel.setPosition(request.position());
        }

        return channelMapper.toResponse(channelRepository.save(channel));
    }

    @Transactional
    public void archive(UUID channelId, UUID requesterId) {
        Channel channel = findChannelOrThrow(channelId);
        workspaceAuthorizationService.requirePermission(channel.getWorkspaceId(), requesterId, "CHANNEL_DELETE");

        channel.setArchivedAt(Instant.now());
        channelRepository.save(channel);

        eventPublisher.publishEvent(new ChannelArchivedEvent(
                channel.getWorkspaceId(), requesterId, channel.getId(), channel.getName()));
    }

    private Channel findChannelOrThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Channel not found"));
    }
}
