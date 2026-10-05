package com.pulse.conversations.repository;

import com.pulse.conversations.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByDirectKey(String directKey);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.id IN (SELECT cm.conversationId FROM ConversationMember cm WHERE cm.userId = :userId)
            """)
    Page<Conversation> findAllForUser(UUID userId, Pageable pageable);
}
