package com.pulse.channels.repository;

import com.pulse.channels.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    List<ChannelMember> findAllByChannelId(UUID channelId);

    void deleteByChannelIdAndUserId(UUID channelId, UUID userId);

    @Modifying
    @Query("""
            DELETE FROM ChannelMember cm
            WHERE cm.userId = :userId
              AND cm.channelId IN (SELECT c.id FROM Channel c WHERE c.workspaceId = :workspaceId)
            """)
    void deleteAllForUserInWorkspace(UUID userId, UUID workspaceId);
}
