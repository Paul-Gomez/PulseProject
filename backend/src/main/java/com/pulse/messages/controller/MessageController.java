package com.pulse.messages.controller;

import com.pulse.common.pagination.PageResponse;
import com.pulse.common.security.CurrentUserProvider;
import com.pulse.messages.dto.EditMessageRequest;
import com.pulse.messages.dto.MessageResponse;
import com.pulse.messages.dto.SendMessageRequest;
import com.pulse.messages.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/api/v1/channels/{channelId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable UUID channelId, @Valid @RequestBody SendMessageRequest request) {
        return messageService.send(channelId, currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping("/api/v1/channels/{channelId}/messages")
    public PageResponse<MessageResponse> list(@PathVariable UUID channelId, Pageable pageable) {
        return messageService.list(channelId, currentUserProvider.requireCurrentUserId(), pageable);
    }

    @PatchMapping("/api/v1/messages/{id}")
    public MessageResponse edit(@PathVariable UUID id, @Valid @RequestBody EditMessageRequest request) {
        return messageService.edit(id, currentUserProvider.requireCurrentUserId(), request);
    }

    @DeleteMapping("/api/v1/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        messageService.delete(id, currentUserProvider.requireCurrentUserId());
    }
}
