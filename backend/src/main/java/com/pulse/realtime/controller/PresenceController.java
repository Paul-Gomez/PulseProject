package com.pulse.realtime.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.realtime.dto.OnlineUsersResponse;
import com.pulse.realtime.service.PresenceService;
import com.pulse.workspaces.service.WorkspaceMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Initial snapshot of who is online; afterwards clients only need the PRESENCE_CHANGED events. */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final WorkspaceMembershipService membershipService;
    private final PresenceService presenceService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public OnlineUsersResponse online(@PathVariable UUID workspaceId) {
        UUID requesterId = currentUserProvider.requireCurrentUserId();
        var online = membershipService.listMemberUserIds(workspaceId, requesterId).stream()
                .filter(presenceService::isOnline)
                .toList();
        return new OnlineUsersResponse(online);
    }
}
