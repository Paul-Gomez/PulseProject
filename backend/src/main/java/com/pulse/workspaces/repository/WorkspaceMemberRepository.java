package com.pulse.workspaces.repository;

import com.pulse.workspaces.entity.WorkspaceMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    Page<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);
}
