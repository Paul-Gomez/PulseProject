package com.pulse.messages.dto;

import com.pulse.files.dto.AttachmentResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID channelId,
        UUID authorId,
        String content,
        UUID parentMessageId,
        Instant editedAt,
        Instant deletedAt,
        Instant createdAt,
        List<AttachmentResponse> attachments
) {
    public MessageResponse withAttachments(List<AttachmentResponse> newAttachments) {
        return new MessageResponse(id, channelId, authorId, content, parentMessageId,
                editedAt, deletedAt, createdAt, newAttachments);
    }
}
