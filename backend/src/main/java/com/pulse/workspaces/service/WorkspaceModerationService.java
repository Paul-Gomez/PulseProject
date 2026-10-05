package com.pulse.workspaces.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.users.repository.UserRepository;
import com.pulse.workspaces.dto.BanResponse;
import com.pulse.workspaces.entity.Workspace;
import com.pulse.workspaces.entity.WorkspaceBan;
import com.pulse.workspaces.entity.WorkspaceMember;
import com.pulse.workspaces.event.MemberBannedEvent;
import com.pulse.workspaces.event.MemberUnbannedEvent;
import com.pulse.workspaces.repository.WorkspaceBanRepository;
import com.pulse.workspaces.repository.WorkspaceMemberRepository;
import com.pulse.workspaces.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceModerationService {

    private static final String BAN_PERMISSION = "MEMBER_BAN";

    private final WorkspaceBanRepository banRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BanResponse ban(UUID workspaceId, UUID requesterId, UUID targetUserId, String reason) {
        WorkspaceMember actor = authorizationService.requirePermission(workspaceId, requesterId, BAN_PERMISSION);

        if (!userRepository.existsById(targetUserId)) {
            throw ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found");
        }
        if (banRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)) {
            throw ApiException.conflict(ErrorCode.ALREADY_BANNED, "User is already banned from this workspace");
        }

        // A person can also be banned before ever joining; if they are a member, they must be below the actor.
        var membership = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId);
        if (targetUserId.equals(requesterId)
                || membership.filter(m -> !RoleHierarchy.outranks(actor.getRole().getName(), m.getRole().getName())).isPresent()) {
            throw ApiException.forbidden(ErrorCode.ACCESS_DENIED, "You cannot ban a member with an equal or higher role");
        }
        membership.ifPresent(memberRepository::delete);

        WorkspaceBan ban = banRepository.save(WorkspaceBan.builder()
                .workspaceId(workspaceId)
                .userId(targetUserId)
                .bannedBy(requesterId)
                .reason(reason)
                .build());

        eventPublisher.publishEvent(new MemberBannedEvent(
                workspaceId, workspaceName(workspaceId), requesterId, targetUserId, reason));
        return toResponse(ban);
    }

    @Transactional
    public void unban(UUID workspaceId, UUID requesterId, UUID targetUserId) {
        authorizationService.requirePermission(workspaceId, requesterId, BAN_PERMISSION);

        WorkspaceBan ban = banRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Ban not found"));
        banRepository.delete(ban);

        eventPublisher.publishEvent(new MemberUnbannedEvent(workspaceId, requesterId, targetUserId));
    }

    public PageResponse<BanResponse> listBans(UUID workspaceId, UUID requesterId, Pageable pageable) {
        authorizationService.requirePermission(workspaceId, requesterId, BAN_PERMISSION);
        return PageResponse.from(banRepository.findAllByWorkspaceId(workspaceId, pageable), this::toResponse);
    }

    private BanResponse toResponse(WorkspaceBan ban) {
        return new BanResponse(ban.getUserId(), ban.getBannedBy(), ban.getReason(), ban.getCreatedAt());
    }

    private String workspaceName(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId).map(Workspace::getName).orElse("");
    }
}
