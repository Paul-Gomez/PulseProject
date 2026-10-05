package com.pulse.channels.repository;

import com.pulse.channels.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findAllByWorkspaceIdAndArchivedAtIsNullOrderByPositionAsc(UUID workspaceId);

    boolean existsByWorkspaceIdAndNameIgnoreCase(UUID workspaceId, String name);
}
