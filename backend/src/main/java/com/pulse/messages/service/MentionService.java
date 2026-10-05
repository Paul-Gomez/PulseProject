package com.pulse.messages.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.messages.entity.Message;
import com.pulse.messages.entity.MessageMention;
import com.pulse.messages.event.UserMentionedEvent;
import com.pulse.messages.repository.MessageMentionRepository;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks for @username mentions in a message's text and stores who was mentioned,
 * so search and notifications can find them without re-parsing the content.
 * Only people who can actually see the channel count: mentioning someone outside a private
 * channel must not leak the message to them.
 */
@Service
@RequiredArgsConstructor
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_.-]{3,50})");
    private static final int PREVIEW_LENGTH = 100;

    private final UserRepository userRepository;
    private final MessageMentionRepository messageMentionRepository;
    private final ChannelAccessService channelAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public void processMentions(Message message, Channel channel) {
        for (String username : extractUsernames(message.getContent())) {
            userRepository.findByUsername(username)
                    .filter(user -> !user.getId().equals(message.getAuthorId()))
                    .filter(user -> channelAccessService.canAccess(channel, user.getId()))
                    .ifPresent(user -> {
                        messageMentionRepository.save(MessageMention.builder()
                                .messageId(message.getId())
                                .mentionedUserId(user.getId())
                                .build());
                        eventPublisher.publishEvent(new UserMentionedEvent(
                                channel.getWorkspaceId(), channel.getId(), message.getId(),
                                message.getAuthorId(), user.getId(), preview(message.getContent())));
                    });
        }
    }

    private Set<String> extractUsernames(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }

    private String preview(String content) {
        return content.length() <= PREVIEW_LENGTH ? content : content.substring(0, PREVIEW_LENGTH) + "...";
    }
}
