package com.pulse.realtime.service;

import com.pulse.realtime.dto.RealtimeEvent;
import com.pulse.realtime.dto.RealtimeEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public <T> void toChannel(UUID channelId, RealtimeEventType type, T payload) {
        messagingTemplate.convertAndSend("/topic/channel." + channelId, new RealtimeEvent<>(type, payload));
    }

    public <T> void toWorkspacePresence(UUID workspaceId, RealtimeEventType type, T payload) {
        messagingTemplate.convertAndSend("/topic/workspace." + workspaceId + ".presence",
                new RealtimeEvent<>(type, payload));
    }
}
