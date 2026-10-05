package com.pulse.files.repository;

import com.pulse.files.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findAllByMessageIdInOrderByCreatedAtAsc(Collection<UUID> messageIds);

    List<Attachment> findAllByMessageId(UUID messageId);
}
