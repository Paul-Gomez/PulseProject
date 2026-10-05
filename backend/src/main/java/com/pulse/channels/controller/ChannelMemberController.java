package com.pulse.channels.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.channels.service.ChannelMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channels/{channelId}/members")
@RequiredArgsConstructor
public class ChannelMemberController {

    private final ChannelMembershipService channelMembershipService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable UUID channelId, @PathVariable UUID userId) {
        channelMembershipService.addMember(channelId, currentUserProvider.requireCurrentUserId(), userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID channelId, @PathVariable UUID userId) {
        channelMembershipService.removeMember(channelId, currentUserProvider.requireCurrentUserId(), userId);
    }
}
