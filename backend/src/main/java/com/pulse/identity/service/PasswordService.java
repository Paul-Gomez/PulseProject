package com.pulse.identity.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.identity.dto.ChangePasswordRequest;
import com.pulse.users.entity.User;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

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

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        authService.revokeAllSessions(userId);
        loginAttemptService.reset(user.getEmail());
    }
}
