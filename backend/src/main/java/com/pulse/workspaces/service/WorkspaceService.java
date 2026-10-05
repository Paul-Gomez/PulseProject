package com.pulse.workspaces.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.workspaces.dto.CreateWorkspaceRequest;
import com.pulse.workspaces.dto.UpdateWorkspaceRequest;
import com.pulse.workspaces.dto.WorkspaceResponse;
import com.pulse.workspaces.entity.Role;
import com.pulse.workspaces.entity.Workspace;
import com.pulse.workspaces.entity.WorkspaceMember;
import com.pulse.workspaces.mapper.WorkspaceMapper;
import com.pulse.workspaces.repository.RoleRepository;
import com.pulse.workspaces.repository.WorkspaceMemberRepository;
import com.pulse.workspaces.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final String OWNER_ROLE = "OWNER";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RoleRepository roleRepository;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceAuthorizationService authorizationService;

    @Transactional
    public WorkspaceResponse create(UUID ownerId, CreateWorkspaceRequest request) {
        Workspace workspace = Workspace.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(ownerId)
                .build();
        workspace = workspaceRepository.save(workspace);

        Role ownerRole = roleRepository.findByName(OWNER_ROLE)
                .orElseThrow(() -> new IllegalStateException("OWNER role not seeded"));

        WorkspaceMember membership = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(ownerId)
                .role(ownerRole)
                .build();
        workspaceMemberRepository.save(membership);

        return workspaceMapper.toResponse(workspace);
    }

    public List<WorkspaceResponse> listForUser(UUID userId) {
        return workspaceRepository.findAllForUser(userId).stream()
                .map(workspaceMapper::toResponse)
                .toList();
    }

    public WorkspaceResponse getById(UUID workspaceId, UUID requesterId) {
        authorizationService.requireMembership(workspaceId, requesterId);
        return workspaceMapper.toResponse(findWorkspaceOrThrow(workspaceId));
    }

    @Transactional
    public WorkspaceResponse update(UUID workspaceId, UUID requesterId, UpdateWorkspaceRequest request) {
        authorizationService.requirePermission(workspaceId, requesterId, "WORKSPACE_EDIT");

        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        if (request.name() != null) {
            workspace.setName(request.name());
        }
        if (request.description() != null) {
            workspace.setDescription(request.description());
        }
        if (request.iconUrl() != null) {
            workspace.setIconUrl(request.iconUrl());
        }

        return workspaceMapper.toResponse(workspaceRepository.save(workspace));
    }

    @Transactional
    public void delete(UUID workspaceId, UUID requesterId) {
        authorizationService.requirePermission(workspaceId, requesterId, "WORKSPACE_DELETE");
        workspaceRepository.deleteById(workspaceId);
    }

    private Workspace findWorkspaceOrThrow(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Workspace not found"));
    }
}
