package com.pulse.users.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.users.dto.UpdateProfileRequest;
import com.pulse.users.dto.UserResponse;
import com.pulse.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    public UserResponse me() {
        return userService.getById(currentUserProvider.requireCurrentUserId());
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }
}
