package com.pulse.files.service;

import com.pulse.channels.entity.Channel;
import com.pulse.channels.repository.ChannelRepository;
import com.pulse.channels.service.ChannelAccessService;
import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.files.config.StorageProperties;
import com.pulse.files.dto.AttachmentDownload;
import com.pulse.files.dto.AttachmentResponse;
import com.pulse.files.entity.Attachment;
import com.pulse.files.mapper.AttachmentMapper;
import com.pulse.files.repository.AttachmentRepository;
import com.pulse.workspaces.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final StorageService storageService;
    private final FileTypeValidator fileTypeValidator;
    private final StorageProperties storageProperties;
    private final ChannelRepository channelRepository;
    private final ChannelAccessService channelAccessService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Transactional
    public AttachmentResponse upload(UUID channelId, UUID userId, MultipartFile file) {
        Channel channel = findChannelOrThrow(channelId);
        channelAccessService.requireAccess(channel, userId);
        workspaceAuthorizationService.requirePermission(channel.getWorkspaceId(), userId, "FILE_UPLOAD");

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE,
                    "File exceeds the maximum allowed size");
        }

        byte[] content = readBytes(file);
        String mimeType = fileTypeValidator.resolveMimeType(content, file.getContentType());

        // The storage key never contains the name the user sent, so it cannot be used for path tricks.
        String storageKey = channel.getWorkspaceId() + "/" + channelId + "/" + UUID.randomUUID();

        Attachment attachment = attachmentRepository.save(Attachment.builder()
                .channelId(channelId)
                .uploadedBy(userId)
                .storageKey(storageKey)
                .fileName(sanitizeFileName(file.getOriginalFilename()))
                .mimeType(mimeType)
                .sizeBytes(content.length)
                .build());

        // Stored last: if it fails, the transaction rolls back and no orphan row is left.
        storageService.store(storageKey, content, mimeType);

        return attachmentMapper.toResponse(attachment);
    }

    @Transactional
    public List<AttachmentResponse> attachToMessage(UUID messageId, UUID channelId, UUID authorId, List<UUID> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }

        List<UUID> distinctIds = attachmentIds.stream().distinct().toList();
        List<Attachment> found = attachmentRepository.findAllById(distinctIds);

        // Missing, someone else's, already used, or from another channel all look the same from outside.
        boolean allUsable = found.size() == distinctIds.size() && found.stream().allMatch(a ->
                a.getMessageId() == null && a.getUploadedBy().equals(authorId) && a.getChannelId().equals(channelId));
        if (!allUsable) {
            throw ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Attachment not found");
        }

        found.forEach(a -> a.setMessageId(messageId));
        return attachmentRepository.saveAll(found).stream().map(attachmentMapper::toResponse).toList();
    }

    public Map<UUID, List<AttachmentResponse>> findForMessages(Collection<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return attachmentRepository.findAllByMessageIdInOrderByCreatedAtAsc(messageIds).stream()
                .map(attachmentMapper::toResponse)
                .collect(Collectors.groupingBy(AttachmentResponse::messageId));
    }

    public AttachmentDownload openForDownload(UUID attachmentId, UUID userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Attachment not found"));

        channelAccessService.requireAccess(findChannelOrThrow(attachment.getChannelId()), userId);

        // Until it is sent with a message, only whoever uploaded it can see it.
        if (attachment.getMessageId() == null && !attachment.getUploadedBy().equals(userId)) {
            throw ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Attachment not found");
        }

        return new AttachmentDownload(attachment.getFileName(), attachment.getMimeType(),
                attachment.getSizeBytes(), storageService.load(attachment.getStorageKey()));
    }

    @Transactional
    public void deleteForMessage(UUID messageId) {
        List<Attachment> attachments = attachmentRepository.findAllByMessageId(messageId);
        for (Attachment attachment : attachments) {
            try {
                storageService.delete(attachment.getStorageKey());
            } catch (StorageException ex) {
                // Do not block deleting the message because the file store is down; the leftover object is harmless.
                log.warn("Could not delete stored file {}: {}", attachment.getStorageKey(), ex.getMessage());
            }
        }
        attachmentRepository.deleteAll(attachments);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new StorageException("Could not read the uploaded file", ex);
        }
    }

    private Channel findChannelOrThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Channel not found"));
    }

    private static String sanitizeFileName(String original) {
        if (original == null) {
            return "file";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            return "file";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(0, MAX_FILE_NAME_LENGTH) : name;
    }
}
