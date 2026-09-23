package com.pulse.common.security;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    public UUID requireCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PulseUserDetails principal)) {
            throw ApiException.unauthorized(ErrorCode.ACCESS_DENIED, "No authenticated user in context");
        }
        return principal.getId();
    }
}
