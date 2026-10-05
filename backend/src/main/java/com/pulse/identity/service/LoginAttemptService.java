package com.pulse.identity.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.ratelimit.RateLimitProperties;
import com.pulse.common.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Brute-force protection: too many failed logins for one email lock that email for a while. */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    public void requireNotLocked(String email) {
        if (rateLimiter.current(key(email)) >= properties.maxFailedLogins()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS,
                    "Too many failed login attempts, try again later");
        }
    }

    public void recordFailure(String email) {
        rateLimiter.hit(key(email), Duration.ofMinutes(properties.loginLockMinutes()));
    }

    public void reset(String email) {
        rateLimiter.reset(key(email));
    }

    private String key(String email) {
        return "loginfail:" + email.toLowerCase();
    }
}
