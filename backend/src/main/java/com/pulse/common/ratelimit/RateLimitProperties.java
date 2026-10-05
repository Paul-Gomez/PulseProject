package com.pulse.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulse.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int authRequestsPerMinute,
        int maxFailedLogins,
        int loginLockMinutes
) {
}
