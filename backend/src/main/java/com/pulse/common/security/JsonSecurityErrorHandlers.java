package com.pulse.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security failures happen in the filter chain, before controllers, so GlobalExceptionHandler never sees them.
 * Without these, a request with no/expired token gets Spring's default 403 instead of a 401 the client can react to.
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Access denied");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                       ErrorCode code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(status.value(), code, message, request.getRequestURI()));
    }
}
