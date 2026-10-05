package com.pulse.notifications.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.notifications.dto.NotificationResponse;
import com.pulse.notifications.entity.Notification;
import com.pulse.notifications.entity.NotificationType;
import com.pulse.notifications.event.NotificationCreatedEvent;
import com.pulse.notifications.mapper.NotificationMapper;
import com.pulse.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_TEXT_LENGTH = 255;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** Optional references (workspace, channel, message, conversation) let the client link to the source. */
    public record Target(UUID workspaceId, UUID channelId, UUID messageId, UUID conversationId) {
        public static Target none() {
            return new Target(null, null, null, null);
        }
    }

    @Transactional
    public void create(UUID userId, NotificationType type, UUID actorId, Target target, String text) {
        Notification notification = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .actorId(actorId)
                .workspaceId(target.workspaceId())
                .channelId(target.channelId())
                .messageId(target.messageId())
                .conversationId(target.conversationId())
                .text(text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH - 3) + "...")
                .build());

        eventPublisher.publishEvent(new NotificationCreatedEvent(userId, notificationMapper.toResponse(notification)));
    }

    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? notificationRepository.findAllByUserIdAndReadAtIsNull(userId, pageable)
                : notificationRepository.findAllByUserId(userId, pageable);
        return PageResponse.from(page, notificationMapper::toResponse);
    }

    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }
}
