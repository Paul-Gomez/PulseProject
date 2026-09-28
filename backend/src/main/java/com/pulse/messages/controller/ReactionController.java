package com.pulse.messages.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.messages.dto.ReactionRequest;
import com.pulse.messages.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages/{messageId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable UUID messageId, @Valid @RequestBody ReactionRequest request) {
        reactionService.add(messageId, currentUserProvider.requireCurrentUserId(), request.emoji());
    }

    @DeleteMapping("/{emoji}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID messageId, @PathVariable String emoji) {
        reactionService.remove(messageId, currentUserProvider.requireCurrentUserId(), emoji);
    }
}
