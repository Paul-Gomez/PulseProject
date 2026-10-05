package com.pulse.realtime.controller;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.realtime.config.RealtimeUser;
import com.pulse.realtime.dto.RealtimeEventType;
import com.pulse.realtime.dto.TypingPayload;
import com.pulse.realtime.service.RealtimePublisher;
import com.pulse.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Typing signals are ephemeral: they are never stored. Clients hide the indicator on their own
 * after a few seconds without a new signal.
 */
@Controller
@RequiredArgsConstructor
public class TypingController {

    private final ChannelRepository channelRepository;
    private final ChannelAccessService channelAccessService;
    private final UserService userService;
    private final RealtimePublisher publisher;

    @MessageMapping("/channel.{channelId}/typing")
    public void typing(@DestinationVariable UUID channelId, Principal principal) {
        UUID userId = RealtimeUser.idOf(principal);

        Channel channel = channelRepository.findById(channelId).orElse(null);
        if (channel == null) {
            return;
        }
        channelAccessService.requireAccess(channel, userId);

        String displayName = userService.getById(userId).displayName();
        publisher.toChannel(channelId, RealtimeEventType.TYPING, new TypingPayload(channelId, userId, displayName));
    }
}
