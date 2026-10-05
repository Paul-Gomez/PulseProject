package com.pulse.realtime.listener;

import com.pulse.realtime.config.RealtimeUser;
import com.pulse.realtime.dto.PresencePayload;
import com.pulse.realtime.dto.RealtimeEventType;
import com.pulse.realtime.service.PresenceService;
import com.pulse.realtime.service.RealtimePublisher;
import com.pulse.users.entity.UserStatus;
import com.pulse.users.service.UserService;
import com.pulse.workspaces.dto.WorkspaceResponse;
import com.pulse.workspaces.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final RealtimePublisher publisher;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        UUID userId = RealtimeUser.idOf(principal);
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();

        if (presenceService.registerSession(userId, sessionId)) {
            changeStatus(userId, UserStatus.ONLINE);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        UUID userId = RealtimeUser.idOf(principal);

        if (presenceService.unregisterSession(userId, event.getSessionId())) {
            changeStatus(userId, UserStatus.OFFLINE);
        }
    }

    private void changeStatus(UUID userId, UserStatus status) {
        userService.updateStatus(userId, status);

        PresencePayload payload = new PresencePayload(userId, status);
        for (WorkspaceResponse workspace : workspaceService.listForUser(userId)) {
            publisher.toWorkspacePresence(workspace.id(), RealtimeEventType.PRESENCE_CHANGED, payload);
        }
    }
}
