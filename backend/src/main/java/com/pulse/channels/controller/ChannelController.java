package com.pulse.channels.controller;

import com.pulse.channels.dto.ChannelResponse;
import com.pulse.channels.dto.CreateChannelRequest;
import com.pulse.channels.dto.UpdateChannelRequest;
import com.pulse.channels.service.ChannelService;
import com.pulse.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/api/v1/workspaces/{workspaceId}/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelResponse create(@PathVariable UUID workspaceId, @Valid @RequestBody CreateChannelRequest request) {
        return channelService.create(workspaceId, currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/channels")
    public List<ChannelResponse> listForWorkspace(@PathVariable UUID workspaceId) {
        return channelService.listForWorkspace(workspaceId, currentUserProvider.requireCurrentUserId());
    }

    @GetMapping("/api/v1/channels/{id}")
    public ChannelResponse getById(@PathVariable UUID id) {
        return channelService.getById(id, currentUserProvider.requireCurrentUserId());
    }

    @PatchMapping("/api/v1/channels/{id}")
    public ChannelResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateChannelRequest request) {
        return channelService.update(id, currentUserProvider.requireCurrentUserId(), request);
    }

    @DeleteMapping("/api/v1/channels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        channelService.archive(id, currentUserProvider.requireCurrentUserId());
    }
}
