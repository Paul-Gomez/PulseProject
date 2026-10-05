package com.pulse.conversations.controller;

import com.pulse.common.pagination.PageResponse;
import com.pulse.common.security.CurrentUserProvider;
import com.pulse.conversations.dto.ConversationResponse;
import com.pulse.conversations.dto.DirectMessageResponse;
import com.pulse.conversations.dto.SendDirectMessageRequest;
import com.pulse.conversations.dto.StartConversationRequest;
import com.pulse.conversations.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUserProvider currentUserProvider;

    /** Idempotent: asking again for a 1:1 chat that already exists returns it. */
    @PostMapping("/api/v1/conversations")
    public ConversationResponse start(@Valid @RequestBody StartConversationRequest request) {
        return conversationService.start(currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping("/api/v1/conversations")
    public PageResponse<ConversationResponse> list(
            @PageableDefault(sort = "lastActivityAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return conversationService.list(currentUserProvider.requireCurrentUserId(), pageable);
    }

    @GetMapping("/api/v1/conversations/{id}/messages")
    public PageResponse<DirectMessageResponse> listMessages(
            @PathVariable UUID id,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return conversationService.listMessages(id, currentUserProvider.requireCurrentUserId(), pageable);
    }

    @PostMapping("/api/v1/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public DirectMessageResponse send(@PathVariable UUID id, @Valid @RequestBody SendDirectMessageRequest request) {
        return conversationService.send(id, currentUserProvider.requireCurrentUserId(), request);
    }

    @PatchMapping("/api/v1/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID id) {
        conversationService.markAsRead(id, currentUserProvider.requireCurrentUserId());
    }

    @DeleteMapping("/api/v1/direct-messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID id) {
        conversationService.deleteMessage(id, currentUserProvider.requireCurrentUserId());
    }
}
