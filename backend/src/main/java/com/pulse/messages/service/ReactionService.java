package com.pulse.messages.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.messages.entity.Message;
import com.pulse.messages.entity.MessageReaction;
import com.pulse.messages.repository.MessageReactionRepository;
import com.pulse.messages.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelAccessService channelAccessService;
    private final MessageReactionRepository messageReactionRepository;

    @Transactional
    public void add(UUID messageId, UUID userId, String emoji) {
        Message message = requireAccessibleMessage(messageId, userId);

        boolean alreadyReacted = messageReactionRepository
                .findByMessageIdAndUserIdAndEmoji(message.getId(), userId, emoji)
                .isPresent();
        if (alreadyReacted) {
            return;
        }

        messageReactionRepository.save(MessageReaction.builder()
                .messageId(message.getId())
                .userId(userId)
                .emoji(emoji)
                .build());
    }

    @Transactional
    public void remove(UUID messageId, UUID userId, String emoji) {
        Message message = requireAccessibleMessage(messageId, userId);

        messageReactionRepository.findByMessageIdAndUserIdAndEmoji(message.getId(), userId, emoji)
                .ifPresent(messageReactionRepository::delete);
    }

    private Message requireAccessibleMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Message not found"));

        Channel channel = channelRepository.findById(message.getChannelId())
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Channel not found"));
        channelAccessService.requireAccess(channel, userId);

        return message;
    }
}
