package com.pulse.audit.repository;

import com.pulse.audit.entity.AuditAction;
import com.pulse.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);

    Page<AuditLog> findAllByWorkspaceIdAndAction(UUID workspaceId, AuditAction action, Pageable pageable);
}
