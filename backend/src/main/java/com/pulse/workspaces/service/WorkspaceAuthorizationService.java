package com.pulse.workspaces.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.workspaces.entity.WorkspaceMember;
import com.pulse.workspaces.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Central place for "can this user do X in this workspace" checks.
 * Controllers/services call this explicitly instead of relying on annotations,
 * so every authorization decision is easy to find and to test.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceAuthorizationService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceMember requireMembership(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> ApiException.forbidden(ErrorCode.ACCESS_DENIED, "Not a member of this workspace"));
    }

    public WorkspaceMember requirePermission(UUID workspaceId, UUID userId, String permissionCode) {
        WorkspaceMember member = requireMembership(workspaceId, userId);
        if (!member.getRole().hasPermission(permissionCode)) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED,
                    "Missing permission " + permissionCode + " in this workspace");
        }
        return member;
    }

    public boolean isMember(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public boolean hasPermission(UUID workspaceId, UUID userId, String permissionCode) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(member -> member.getRole().hasPermission(permissionCode))
                .orElse(false);
    }
}
