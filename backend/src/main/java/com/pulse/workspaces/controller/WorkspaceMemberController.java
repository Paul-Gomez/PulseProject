package com.pulse.workspaces.controller;

import com.pulse.common.pagination.PageResponse;
import com.pulse.common.security.CurrentUserProvider;
import com.pulse.workspaces.dto.InviteMemberRequest;
import com.pulse.workspaces.dto.MemberResponse;
import com.pulse.workspaces.dto.UpdateMemberRoleRequest;
import com.pulse.workspaces.service.WorkspaceMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMembershipService membershipService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse invite(@PathVariable UUID workspaceId, @Valid @RequestBody InviteMemberRequest request) {
        return membershipService.invite(workspaceId, currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping
    public PageResponse<MemberResponse> list(@PathVariable UUID workspaceId, Pageable pageable) {
        return membershipService.listMembers(workspaceId, currentUserProvider.requireCurrentUserId(), pageable);
    }

    @PatchMapping("/{userId}/role")
    public MemberResponse updateRole(@PathVariable UUID workspaceId, @PathVariable UUID userId,
                                      @Valid @RequestBody UpdateMemberRoleRequest request) {
        return membershipService.updateRole(workspaceId, currentUserProvider.requireCurrentUserId(), userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID workspaceId, @PathVariable UUID userId) {
        membershipService.remove(workspaceId, currentUserProvider.requireCurrentUserId(), userId);
    }
}
