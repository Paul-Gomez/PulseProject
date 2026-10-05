package com.pulse.workspaces.repository;

import com.pulse.workspaces.entity.WorkspaceBan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceBanRepository extends JpaRepository<WorkspaceBan, UUID> {

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    Optional<WorkspaceBan> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    Page<WorkspaceBan> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);
}
