package com.pulse.conversations.repository;

import com.pulse.conversations.entity.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    Page<DirectMessage> findAllByConversationIdAndDeletedAtIsNull(UUID conversationId, Pageable pageable);

    /** Rows of [conversationId, unreadCount]: other people's messages newer than the user's last read mark. */
    @Query("""
            SELECT dm.conversationId, COUNT(dm)
            FROM DirectMessage dm, ConversationMember cm
            WHERE cm.conversationId = dm.conversationId
              AND cm.userId = :userId
              AND dm.conversationId IN :conversationIds
              AND dm.authorId <> :userId
              AND dm.deletedAt IS NULL
              AND (cm.lastReadAt IS NULL OR dm.createdAt > cm.lastReadAt)
            GROUP BY dm.conversationId
            """)
    List<Object[]> countUnread(UUID userId, Collection<UUID> conversationIds);
}
