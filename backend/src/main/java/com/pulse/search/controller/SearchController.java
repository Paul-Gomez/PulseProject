package com.pulse.search.controller;

import com.pulse.common.pagination.PageResponse;
import com.pulse.common.security.CurrentUserProvider;
import com.pulse.search.dto.SearchCriteria;
import com.pulse.search.dto.SearchResultResponse;
import com.pulse.search.service.MessageSearchService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final MessageSearchService searchService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/messages")
    public PageResponse<SearchResultResponse> searchMessages(
            @RequestParam(name = "q", required = false) @Size(max = 100) String text,
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) UUID channelId,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) UUID mentionedUserId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SearchCriteria criteria = new SearchCriteria(text, workspaceId, channelId, authorId, mentionedUserId, from, to);
        return searchService.search(currentUserProvider.requireCurrentUserId(), criteria, pageable);
    }
}
