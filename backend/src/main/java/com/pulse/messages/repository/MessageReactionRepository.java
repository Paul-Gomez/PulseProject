package com.pulse.messages.repository;

import com.pulse.messages.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);
}
