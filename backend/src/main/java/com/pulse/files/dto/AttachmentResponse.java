package com.pulse.files.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID channelId,
        UUID messageId,
        String fileName,
        String mimeType,
        long sizeBytes,
        Instant createdAt
) {
}
