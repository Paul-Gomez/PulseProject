package com.pulse.files.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.files.dto.AttachmentDownload;
import com.pulse.files.dto.AttachmentResponse;
import com.pulse.files.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/api/v1/channels/{channelId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@PathVariable UUID channelId, @RequestParam("file") MultipartFile file) {
        return attachmentService.upload(channelId, currentUserProvider.requireCurrentUserId(), file);
    }

    @GetMapping("/api/v1/attachments/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        AttachmentDownload download = attachmentService.openForDownload(id, currentUserProvider.requireCurrentUserId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(download.content()));
    }
}
