package com.pulse.conversations.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.conversations.dto.ConversationResponse;
import com.pulse.conversations.dto.DirectMessageResponse;
import com.pulse.conversations.dto.SendDirectMessageRequest;
import com.pulse.conversations.dto.StartConversationRequest;
import com.pulse.conversations.entity.Conversation;
import com.pulse.conversations.entity.ConversationMember;
import com.pulse.conversations.entity.DirectMessage;
import com.pulse.conversations.event.DirectMessageCreatedEvent;
import com.pulse.conversations.event.DirectMessageDeletedEvent;
import com.pulse.conversations.mapper.DirectMessageMapper;
import com.pulse.conversations.repository.ConversationMemberRepository;
import com.pulse.conversations.repository.ConversationRepository;
import com.pulse.conversations.repository.DirectMessageRepository;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageMapper directMessageMapper;
    private final ConversationAccessService accessService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Starts a conversation, or returns the existing 1:1 one. Not @Transactional on purpose: if two people
     * open the same chat at the same time, the loser hits the unique constraint, and a transaction that has
     * failed on a constraint cannot be reused, so the insert runs in its own short transaction.
     */
    public ConversationResponse start(UUID requesterId, StartConversationRequest request) {
        Set<UUID> others = new LinkedHashSet<>(request.participantIds());
        others.remove(requesterId);
        if (others.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "You cannot start a conversation with yourself");
        }
        for (UUID other : others) {
            if (!workspaceAuthorizationService.shareWorkspace(requesterId, other)) {
                throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "You can only message people you share a workspace with");
            }
        }

        if (others.size() > 1) {
            Conversation group = transactionTemplate.execute(status -> create(requesterId, others, true, null, request.name()));
            return toResponse(group, requesterId);
        }

        UUID other = others.iterator().next();
        String key = directKey(requesterId, other);
        Conversation conversation = conversationRepository.findByDirectKey(key).orElse(null);
        if (conversation == null) {
            try {
                conversation = transactionTemplate.execute(status -> create(requesterId, others, false, key, null));
            } catch (DataIntegrityViolationException raceLost) {
                conversation = conversationRepository.findByDirectKey(key).orElseThrow(() -> raceLost);
            }
        }
        return toResponse(conversation, requesterId);
    }

    public PageResponse<ConversationResponse> list(UUID userId, Pageable pageable) {
        Page<Conversation> page = conversationRepository.findAllForUser(userId, pageable);
        List<UUID> ids = page.getContent().stream().map(Conversation::getId).toList();

        Map<UUID, List<UUID>> membersByConversation = ids.isEmpty() ? Map.of()
                : memberRepository.findAllByConversationIdIn(ids).stream().collect(
                        Collectors.groupingBy(ConversationMember::getConversationId,
                                Collectors.mapping(ConversationMember::getUserId, Collectors.toList())));
        Map<UUID, Long> unread = unreadByConversation(userId, ids);

        return PageResponse.from(page, c -> toResponse(c,
                membersByConversation.getOrDefault(c.getId(), List.of()), unread.getOrDefault(c.getId(), 0L)));
    }

    public PageResponse<DirectMessageResponse> listMessages(UUID conversationId, UUID userId, Pageable pageable) {
        accessService.requireMember(conversationId, userId);
        return PageResponse.from(
                directMessageRepository.findAllByConversationIdAndDeletedAtIsNull(conversationId, pageable),
                directMessageMapper::toResponse);
    }

    @Transactional
    public DirectMessageResponse send(UUID conversationId, UUID authorId, SendDirectMessageRequest request) {
        Conversation conversation = accessService.requireMember(conversationId, authorId);

        if (request.parentMessageId() != null) {
            DirectMessage parent = directMessageRepository.findById(request.parentMessageId())
                    .filter(m -> m.getConversationId().equals(conversationId))
                    .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Parent message not found"));
            if (parent.getDeletedAt() != null) {
                throw ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Parent message not found");
            }
        }

        DirectMessage saved = directMessageRepository.save(DirectMessage.builder()
                .conversationId(conversationId)
                .authorId(authorId)
                .content(request.content())
                .parentMessageId(request.parentMessageId())
                .build());

        conversation.setLastActivityAt(Instant.now());
        conversationRepository.save(conversation);

        DirectMessageResponse response = directMessageMapper.toResponse(saved);
        List<UUID> memberIds = memberRepository.findAllByConversationId(conversationId).stream()
                .map(ConversationMember::getUserId).toList();
        eventPublisher.publishEvent(new DirectMessageCreatedEvent(response, memberIds));
        return response;
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID requesterId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Message not found"));
        accessService.requireMember(message.getConversationId(), requesterId);

        if (!message.getAuthorId().equals(requesterId)) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "Only the author can delete a message");
        }

        message.setDeletedAt(Instant.now());
        directMessageRepository.save(message);
        eventPublisher.publishEvent(new DirectMessageDeletedEvent(message.getConversationId(), message.getId()));
    }

    @Transactional
    public void markAsRead(UUID conversationId, UUID userId) {
        accessService.requireMember(conversationId, userId);
        ConversationMember member = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Conversation not found"));
        member.setLastReadAt(Instant.now());
        memberRepository.save(member);
    }

    private Conversation create(UUID creatorId, Set<UUID> others, boolean group, String directKey, String name) {
        Conversation conversation = conversationRepository.saveAndFlush(Conversation.builder()
                .group(group)
                .name(name)
                .directKey(directKey)
                .createdBy(creatorId)
                .lastActivityAt(Instant.now())
                .build());

        memberRepository.save(ConversationMember.builder().conversationId(conversation.getId()).userId(creatorId).build());
        others.forEach(userId -> memberRepository.save(
                ConversationMember.builder().conversationId(conversation.getId()).userId(userId).build()));
        return conversation;
    }

    private ConversationResponse toResponse(Conversation conversation, UUID userId) {
        List<UUID> memberIds = memberRepository.findAllByConversationId(conversation.getId()).stream()
                .map(ConversationMember::getUserId).toList();
        long unread = unreadByConversation(userId, List.of(conversation.getId())).getOrDefault(conversation.getId(), 0L);
        return toResponse(conversation, memberIds, unread);
    }

    private ConversationResponse toResponse(Conversation c, List<UUID> memberIds, long unread) {
        return new ConversationResponse(c.getId(), c.isGroup(), c.getName(), memberIds, unread,
                c.getCreatedAt(), c.getLastActivityAt());
    }

    private Map<UUID, Long> unreadByConversation(UUID userId, Collection<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        return directMessageRepository.countUnread(userId, conversationIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    private static String directKey(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }
}
