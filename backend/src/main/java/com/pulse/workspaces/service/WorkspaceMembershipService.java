package com.pulse.workspaces.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.users.repository.UserRepository;
import com.pulse.workspaces.dto.InviteMemberRequest;
import com.pulse.workspaces.dto.MemberResponse;
import com.pulse.workspaces.dto.UpdateMemberRoleRequest;
import com.pulse.workspaces.entity.Role;
import com.pulse.workspaces.entity.WorkspaceMember;
import com.pulse.workspaces.mapper.MemberMapper;
import com.pulse.workspaces.repository.RoleRepository;
import com.pulse.workspaces.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceMembershipService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberMapper memberMapper;

    @Transactional
    public MemberResponse invite(UUID workspaceId, UUID requesterId, InviteMemberRequest request) {
        authorizationService.requirePermission(workspaceId, requesterId, "MEMBER_INVITE");

        var invitedUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitedUser.getId())) {
            throw ApiException.conflict(ErrorCode.MEMBER_ALREADY_EXISTS, "User is already a member of this workspace");
        }

        Role memberRole = roleRepository.findByName("MEMBER")
                .orElseThrow(() -> new IllegalStateException("MEMBER role not seeded"));

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(invitedUser.getId())
                .role(memberRole)
                .build();

        return memberMapper.toResponse(workspaceMemberRepository.save(member));
    }

    public PageResponse<MemberResponse> listMembers(UUID workspaceId, UUID requesterId, Pageable pageable) {
        authorizationService.requireMembership(workspaceId, requesterId);
        var page = workspaceMemberRepository.findAllByWorkspaceId(workspaceId, pageable);
        return PageResponse.from(page, memberMapper::toResponse);
    }

    @Transactional
    public MemberResponse updateRole(UUID workspaceId, UUID requesterId, UUID targetUserId, UpdateMemberRoleRequest request) {
        authorizationService.requirePermission(workspaceId, requesterId, "MEMBER_MANAGE_ROLES");

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Member not found"));

        Role newRole = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));

        member.setRole(newRole);
        return memberMapper.toResponse(workspaceMemberRepository.save(member));
    }

    @Transactional
    public void remove(UUID workspaceId, UUID requesterId, UUID targetUserId) {
        authorizationService.requirePermission(workspaceId, requesterId, "MEMBER_REMOVE");

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Member not found"));

        workspaceMemberRepository.delete(member);
    }
}
