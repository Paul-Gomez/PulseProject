package com.pulse.identity.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.identity.dto.ChangePasswordRequest;
import com.pulse.identity.service.EmailVerificationService;
import com.pulse.identity.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class AccountController {

    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;
    private final CurrentUserProvider currentUserProvider;

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(currentUserProvider.requireCurrentUserId(), request);
    }

    @PostMapping("/verification-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerificationEmail() {
        emailVerificationService.resend(currentUserProvider.requireCurrentUserId());
    }
}
