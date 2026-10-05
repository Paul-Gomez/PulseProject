package com.pulse.realtime.config;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.common.exception.ApiException;
import com.pulse.common.security.JwtService;
import com.pulse.common.security.PulseUserDetails;
import com.pulse.users.repository.UserRepository;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Authenticates the STOMP CONNECT frame with the same JWT used by the REST API and
 * checks every SUBSCRIBE, so a user can only listen to channels/workspaces they can access.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHANNEL_TOPIC_PREFIX = "/topic/channel.";
    private static final String WORKSPACE_TOPIC_PREFIX = "/topic/workspace.";
    private static final String PRESENCE_SUFFIX = ".presence";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ChannelAccessService channelAccessService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("Missing bearer token");
        }

        String token = header.substring(BEARER_PREFIX.length());
        if (!jwtService.isValid(token)) {
            throw new MessageDeliveryException("Invalid token");
        }

        UUID userId = jwtService.extractUserId(token);
        PulseUserDetails principal = userRepository.findById(userId)
                .map(PulseUserDetails::new)
                .orElseThrow(() -> new MessageDeliveryException("Unknown user"));

        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        UUID userId = RealtimeUser.idOf(accessor.getUser());
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessageDeliveryException("Missing destination");
        }

        try {
            if (destination.startsWith(CHANNEL_TOPIC_PREFIX)) {
                UUID channelId = UUID.fromString(destination.substring(CHANNEL_TOPIC_PREFIX.length()));
                Channel target = channelRepository.findById(channelId)
                        .orElseThrow(() -> new MessageDeliveryException("Unknown channel"));
                channelAccessService.requireAccess(target, userId);
            } else if (destination.startsWith(WORKSPACE_TOPIC_PREFIX) && destination.endsWith(PRESENCE_SUFFIX)) {
                String id = destination.substring(WORKSPACE_TOPIC_PREFIX.length(),
                        destination.length() - PRESENCE_SUFFIX.length());
                workspaceAuthorizationService.requireMembership(UUID.fromString(id), userId);
            } else if (destination.startsWith("/topic/")) {
                throw new MessageDeliveryException("Subscription not allowed: " + destination);
            }
        } catch (ApiException | IllegalArgumentException ex) {
            throw new MessageDeliveryException("Subscription not allowed: " + destination);
        }
    }
}
