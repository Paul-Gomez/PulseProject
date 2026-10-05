package com.pulse.workspaces.repository;

import com.pulse.workspaces.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("""
            SELECT w FROM Workspace w
            JOIN WorkspaceMember wm ON wm.workspaceId = w.id
            WHERE wm.userId = :userId
            ORDER BY w.createdAt DESC
            """)
    List<Workspace> findAllForUser(UUID userId);
}
