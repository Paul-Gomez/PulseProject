package com.pulse.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        String path,
        List<String> details
) {
    public static ErrorResponse of(int status, ErrorCode code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, List.of());
    }

    public static ErrorResponse of(int status, ErrorCode code, String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status, code, message, path, details);
    }
}
