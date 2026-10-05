package com.pulse.messages.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.files.dto.AttachmentResponse;
import com.pulse.files.service.AttachmentService;
import com.pulse.messages.dto.EditMessageRequest;
import com.pulse.messages.dto.MessageResponse;
import com.pulse.messages.dto.SendMessageRequest;
import com.pulse.messages.entity.Message;
import com.pulse.messages.event.MessageCreatedEvent;
import com.pulse.messages.event.MessageDeletedEvent;
import com.pulse.messages.event.MessageUpdatedEvent;
import com.pulse.messages.mapper.MessageMapper;
import com.pulse.messages.repository.MessageRepository;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;

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
        List<AttachmentResponse> attachments = attachmentService.attachToMessage(
                message.getId(), channelId, authorId, request.attachmentIds());

        MessageResponse response = messageMapper.toResponse(message).withAttachments(attachments);
        eventPublisher.publishEvent(new MessageCreatedEvent(response));
        return response;
    }

    public PageResponse<MessageResponse> list(UUID channelId, UUID requesterId, Pageable pageable) {
        Channel channel = findChannelOrThrow(channelId);
        channelAccessService.requireAccess(channel, requesterId);

        var page = messageRepository.findAllByChannelIdAndDeletedAtIsNullOrderByCreatedAtDesc(channelId, pageable);
        List<UUID> messageIds = page.getContent().stream().map(Message::getId).toList();
        Map<UUID, List<AttachmentResponse>> attachments = attachmentService.findForMessages(messageIds);

        return PageResponse.from(page, message ->
                messageMapper.toResponse(message).withAttachments(attachments.getOrDefault(message.getId(), List.of())));
    }

    @Transactional
    public MessageResponse edit(UUID messageId, UUID requesterId, EditMessageRequest request) {
        Message message = findMessageOrThrow(messageId);
        Channel channel = findChannelOrThrow(message.getChannelId());
        channelAccessService.requireAccess(channel, requesterId);

        if (message.isDeleted()) {
            throw ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Message not found");
        }
        if (!message.getAuthorId().equals(requesterId)) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "Only the author can edit a message");
        }

        message.setContent(request.content());
        message.setEditedAt(Instant.now());
        message = messageRepository.save(message);

        List<AttachmentResponse> attachments = attachmentService.findForMessages(List.of(message.getId()))
                .getOrDefault(message.getId(), List.of());
        MessageResponse response = messageMapper.toResponse(message).withAttachments(attachments);
        eventPublisher.publishEvent(new MessageUpdatedEvent(response));
        return response;
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
        attachmentService.deleteForMessage(message.getId());

        eventPublisher.publishEvent(new MessageDeletedEvent(message.getChannelId(), message.getId()));
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
