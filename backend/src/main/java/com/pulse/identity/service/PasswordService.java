package com.pulse.identity.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.mail.EmailService;
import com.pulse.common.mail.MailProperties;
import com.pulse.common.ratelimit.RateLimiter;
import com.pulse.identity.dto.ChangePasswordRequest;
import com.pulse.identity.dto.ResetPasswordRequest;
import com.pulse.identity.entity.AccountTokenType;
import com.pulse.users.entity.User;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;
    private final AccountTokenService tokenService;
    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final RateLimiter rateLimiter;

    /**
     * Changing the password signs the account out everywhere: every refresh token is revoked, so the user
     * (and anyone who had stolen a session) has to log in again once the short-lived access tokens expire.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));

        // Same counter as login: a stolen access token must not allow guessing the current password.
        loginAttemptService.requireNotLocked(user.getEmail());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(user.getEmail());
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PASSWORD, "Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                    "The new password must be different from the current one");
        }

        updatePassword(user, request.newPassword());
    }

    /**
     * Always completes the same way whether or not the email exists, so this cannot be used to find out who is
     * registered. At most 3 emails per hour per address, so it cannot be used to flood someone's inbox either.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!rateLimiter.tryAcquire("forgot-password:" + email.toLowerCase(), 3, Duration.ofHours(1))) {
                return;
            }
            String token = tokenService.issue(user.getId(), AccountTokenType.RESET_PASSWORD, RESET_TOKEN_LIFETIME);
            try {
                emailService.send(user.getEmail(), "Reset your Pulse password",
                        "Hi " + user.getDisplayName() + ",\n\n"
                                + "Someone asked to reset the password of this account. Choose a new one here:\n"
                                + mailProperties.frontendUrl() + "/reset-password?token=" + token + "\n\n"
                                + "The link works once and expires in 1 hour. If it was not you, ignore this message.");
            } catch (RuntimeException ex) {
                log.warn("Could not send password reset email to user {}: {}", user.getId(), ex.getMessage());
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UUID userId = tokenService.consume(request.token(), AccountTokenType.RESET_PASSWORD);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));
        updatePassword(user, request.newPassword());
    }

    private void updatePassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        authService.revokeAllSessions(user.getId());
        loginAttemptService.reset(user.getEmail());
    }
}
