package com.pulse.workspaces.controller;

import com.pulse.common.pagination.PageResponse;
import com.pulse.common.security.CurrentUserProvider;
import com.pulse.workspaces.dto.BanMemberRequest;
import com.pulse.workspaces.dto.BanResponse;
import com.pulse.workspaces.service.WorkspaceModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/bans")
@RequiredArgsConstructor
public class WorkspaceBanController {

    private final WorkspaceModerationService moderationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BanResponse ban(@PathVariable UUID workspaceId, @PathVariable UUID userId,
                           @Valid @RequestBody(required = false) BanMemberRequest request) {
        String reason = request == null ? null : request.reason();
        return moderationService.ban(workspaceId, currentUserProvider.requireCurrentUserId(), userId, reason);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unban(@PathVariable UUID workspaceId, @PathVariable UUID userId) {
        moderationService.unban(workspaceId, currentUserProvider.requireCurrentUserId(), userId);
    }

    @GetMapping
    public PageResponse<BanResponse> list(@PathVariable UUID workspaceId, Pageable pageable) {
        return moderationService.listBans(workspaceId, currentUserProvider.requireCurrentUserId(), pageable);
    }
}
