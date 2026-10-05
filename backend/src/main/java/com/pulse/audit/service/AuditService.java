package com.pulse.audit.service;

import com.pulse.audit.dto.AuditLogResponse;
import com.pulse.audit.entity.AuditAction;
import com.pulse.audit.entity.AuditLog;
import com.pulse.audit.mapper.AuditLogMapper;
import com.pulse.audit.repository.AuditLogRepository;
import com.pulse.common.pagination.PageResponse;
import com.pulse.users.service.UserService;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String AUDIT_PERMISSION = "AUDIT_VIEW";

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final UserService userService;
    private final WorkspaceAuthorizationService authorizationService;

    /** The actor's username is copied into the entry, so the trail still reads correctly if they rename later. */
    @Transactional
    public void record(UUID workspaceId, UUID actorId, AuditAction action, String resourceType,
                       UUID resourceId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .workspaceId(workspaceId)
                .actorId(actorId)
                .actorUsername(actorId == null ? null : userService.getById(actorId).username())
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .build());
    }

    public PageResponse<AuditLogResponse> list(UUID workspaceId, UUID requesterId, AuditAction action, Pageable pageable) {
        authorizationService.requirePermission(workspaceId, requesterId, AUDIT_PERMISSION);

        var page = action == null
                ? auditLogRepository.findAllByWorkspaceId(workspaceId, pageable)
                : auditLogRepository.findAllByWorkspaceIdAndAction(workspaceId, action, pageable);
        return PageResponse.from(page, auditLogMapper::toResponse);
    }
}
