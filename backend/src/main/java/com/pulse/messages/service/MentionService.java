package com.pulse.messages.service;

import com.pulse.messages.entity.MessageMention;
import com.pulse.messages.repository.MessageMentionRepository;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks for @username mentions in a message's text and stores who was mentioned,
 * so search and (later) notifications can find them without re-parsing the content.
 */
@Service
@RequiredArgsConstructor
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_.-]{3,50})");

    private final UserRepository userRepository;
    private final MessageMentionRepository messageMentionRepository;

    public void processMentions(UUID messageId, String content) {
        Set<String> usernames = extractUsernames(content);

        for (String username : usernames) {
            userRepository.findByUsername(username).ifPresent(user ->
                    messageMentionRepository.save(MessageMention.builder()
                            .messageId(messageId)
                            .mentionedUserId(user.getId())
                            .build()));
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
}
