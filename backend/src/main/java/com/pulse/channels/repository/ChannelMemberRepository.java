package com.pulse.channels.repository;

import com.pulse.channels.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    List<ChannelMember> findAllByChannelId(UUID channelId);

    void deleteByChannelIdAndUserId(UUID channelId, UUID userId);
}
