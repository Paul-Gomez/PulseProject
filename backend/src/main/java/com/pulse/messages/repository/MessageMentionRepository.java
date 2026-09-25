package com.pulse.messages.repository;

import com.pulse.messages.entity.MessageMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageMentionRepository extends JpaRepository<MessageMention, UUID> {
}
