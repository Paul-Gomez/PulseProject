package com.pulse.conversations.repository;

import com.pulse.conversations.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    List<ConversationMember> findAllByConversationId(UUID conversationId);

    List<ConversationMember> findAllByConversationIdIn(Collection<UUID> conversationIds);

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);
}
