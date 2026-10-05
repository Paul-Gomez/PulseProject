package com.pulse.audit.controller;

import com.pulse.audit.dto.AuditLogResponse;
import com.pulse.audit.entity.AuditAction;
import com.pulse.audit.service.AuditService;
import com.pulse.common.pagination.PageResponse;
import com.pulse.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditService.list(workspaceId, currentUserProvider.requireCurrentUserId(), action, pageable);
    }
}
