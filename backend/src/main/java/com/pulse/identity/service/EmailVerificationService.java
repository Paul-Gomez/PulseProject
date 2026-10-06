package com.pulse.identity.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.mail.EmailService;
import com.pulse.common.mail.MailProperties;
import com.pulse.common.ratelimit.RateLimiter;
import com.pulse.identity.entity.AccountTokenType;
import com.pulse.users.entity.User;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    private final AccountTokenService tokenService;
    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;

    /** A mail outage must not stop someone from registering, so failures are logged and not propagated. */
    public void sendVerificationEmail(User user) {
        String token = tokenService.issue(user.getId(), AccountTokenType.VERIFY_EMAIL, TOKEN_LIFETIME);
        try {
            emailService.send(user.getEmail(), "Verify your Pulse email",
                    "Hi " + user.getDisplayName() + ",\n\n"
                            + "Confirm your email address to finish setting up your account:\n"
                            + mailProperties.frontendUrl() + "/verify-email?token=" + token + "\n\n"
                            + "The link works once and expires in 24 hours. If you did not create this account, ignore this message.");
        } catch (RuntimeException ex) {
            log.warn("Could not send verification email to user {}: {}", user.getId(), ex.getMessage());
        }
    }

    @Transactional
    public void verify(String rawToken) {
        UUID userId = tokenService.consume(rawToken, AccountTokenType.VERIFY_EMAIL);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resend(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));
        if (user.isEmailVerified()) {
            return;
        }
        if (!rateLimiter.tryAcquire("verification-resend:" + userId, 3, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED,
                    "Too many verification emails requested, try again later");
        }
        sendVerificationEmail(user);
    }
}
