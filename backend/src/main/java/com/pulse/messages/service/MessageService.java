package com.pulse.messages.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.messages.dto.EditMessageRequest;
import com.pulse.messages.dto.MessageResponse;
import com.pulse.messages.dto.SendMessageRequest;
import com.pulse.messages.entity.Message;
import com.pulse.messages.mapper.MessageMapper;
import com.pulse.messages.repository.MessageRepository;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelAccessService channelAccessService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final MessageMapper messageMapper;
    private final MentionService mentionService;

    @Transactional
    public MessageResponse send(UUID channelId, UUID authorId, SendMessageRequest request) {
        Channel channel = findChannelOrThrow(channelId);
        channelAccessService.requireAccess(channel, authorId);

        if (request.parentMessageId() != null) {
            Message parent = messageRepository.findById(request.parentMessageId())
                    .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Parent message not found"));
            if (!parent.getChannelId().equals(channelId)) {
                throw ApiException.conflict(ErrorCode.RESOURCE_NOT_FOUND, "Parent message belongs to a different channel");
            }
        }

        Message message = Message.builder()
                .channelId(channelId)
                .authorId(authorId)
                .content(request.content())
                .parentMessageId(request.parentMessageId())
                .build();
        message = messageRepository.save(message);

        mentionService.processMentions(message.getId(), message.getContent());

        return messageMapper.toResponse(message);
    }

    public PageResponse<MessageResponse> list(UUID channelId, UUID requesterId, Pageable pageable) {
        Channel channel = findChannelOrThrow(channelId);
        channelAccessService.requireAccess(channel, requesterId);

        var page = messageRepository.findAllByChannelIdAndDeletedAtIsNullOrderByCreatedAtDesc(channelId, pageable);
        return PageResponse.from(page, messageMapper::toResponse);
    }

    @Transactional
    public MessageResponse edit(UUID messageId, UUID requesterId, EditMessageRequest request) {
        Message message = findMessageOrThrow(messageId);
        Channel channel = findChannelOrThrow(message.getChannelId());
        channelAccessService.requireAccess(channel, requesterId);

        message.setContent(request.content());
        message.setEditedAt(Instant.now());
        message = messageRepository.save(message);

        return messageMapper.toResponse(message);
    }

    @Transactional
    public void delete(UUID messageId, UUID requesterId) {
        Message message = findMessageOrThrow(messageId);
        Channel channel = findChannelOrThrow(message.getChannelId());
        channelAccessService.requireAccess(channel, requesterId);

        boolean isAuthor = message.getAuthorId().equals(requesterId);
        boolean canDeleteAny = workspaceAuthorizationService.hasPermission(
                channel.getWorkspaceId(), requesterId, "MESSAGE_DELETE_ANY");

        if (!isAuthor && !canDeleteAny) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "Cannot delete someone else's message");
        }

        message.setDeletedAt(Instant.now());
        messageRepository.save(message);
    }

    private Message findMessageOrThrow(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Message not found"));
    }

    private Channel findChannelOrThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Channel not found"));
    }
}
