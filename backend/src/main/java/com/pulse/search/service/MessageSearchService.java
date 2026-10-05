package com.pulse.search.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.pagination.PageResponse;
import com.pulse.search.dto.SearchCriteria;
import com.pulse.search.dto.SearchResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Read-only search over messages. It is plain SQL on purpose: the filters are optional and combine freely,
 * and the visibility rule (only channels the user can read) is easier to audit in one query than spread
 * across repositories of other modules.
 */
@Service
@RequiredArgsConstructor
public class MessageSearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final String VISIBLE_TO_USER = """
            m.deleted_at IS NULL
            AND EXISTS (SELECT 1 FROM workspace_members wm
                        WHERE wm.workspace_id = c.workspace_id AND wm.user_id = :userId)
            AND (c.is_private = FALSE
                 OR EXISTS (SELECT 1 FROM channel_members cm WHERE cm.channel_id = c.id AND cm.user_id = :userId))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PageResponse<SearchResultResponse> search(UUID userId, SearchCriteria criteria, Pageable pageable) {
        String direction = sortDirection(pageable);
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int page = pageable.getPageNumber();

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        String where = VISIBLE_TO_USER + filters(criteria, params);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages m JOIN channels c ON c.id = m.channel_id WHERE " + where,
                params, Long.class);

        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);
        List<SearchResultResponse> results = jdbc.query("""
                SELECT m.id, m.channel_id, c.name AS channel_name, c.workspace_id, m.author_id,
                       u.display_name, m.content, m.created_at
                FROM messages m
                JOIN channels c ON c.id = m.channel_id
                JOIN users u ON u.id = m.author_id
                WHERE %s
                ORDER BY m.created_at %s, m.id
                LIMIT :limit OFFSET :offset
                """.formatted(where, direction), params, (rs, rowNum) -> new SearchResultResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("channel_id", UUID.class),
                rs.getString("channel_name"),
                rs.getObject("workspace_id", UUID.class),
                rs.getObject("author_id", UUID.class),
                rs.getString("display_name"),
                rs.getString("content"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()));

        long totalElements = total == null ? 0 : total;
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(results, page, size, totalElements, totalPages);
    }

    private String filters(SearchCriteria criteria, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();
        if (criteria.text() != null && !criteria.text().isBlank()) {
            sql.append(" AND m.content ILIKE :text ESCAPE '\\'");
            params.addValue("text", "%" + escapeLike(criteria.text().trim()) + "%");
        }
        if (criteria.workspaceId() != null) {
            sql.append(" AND c.workspace_id = :workspaceId");
            params.addValue("workspaceId", criteria.workspaceId());
        }
        if (criteria.channelId() != null) {
            sql.append(" AND m.channel_id = :channelId");
            params.addValue("channelId", criteria.channelId());
        }
        if (criteria.authorId() != null) {
            sql.append(" AND m.author_id = :authorId");
            params.addValue("authorId", criteria.authorId());
        }
        if (criteria.mentionedUserId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM message_mentions mm"
                    + " WHERE mm.message_id = m.id AND mm.mentioned_user_id = :mentionedUserId)");
            params.addValue("mentionedUserId", criteria.mentionedUserId());
        }
        if (criteria.from() != null) {
            sql.append(" AND m.created_at >= :from");
            params.addValue("from", OffsetDateTime.ofInstant(criteria.from(), ZoneOffset.UTC));
        }
        if (criteria.to() != null) {
            sql.append(" AND m.created_at < :to");
            params.addValue("to", OffsetDateTime.ofInstant(criteria.to(), ZoneOffset.UTC));
        }
        return sql.toString();
    }

    // Only createdAt can be sorted on; the direction is mapped here so no client text ever reaches the SQL.
    private String sortDirection(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "DESC";
        }
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        if (order == null || pageable.getSort().stream().count() > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Search results can only be sorted by createdAt");
        }
        return order.isAscending() ? "ASC" : "DESC";
    }

    private String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
