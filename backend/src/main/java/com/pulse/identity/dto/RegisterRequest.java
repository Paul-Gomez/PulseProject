package com.pulse.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,50}$", message = "invalid username format") String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 1, max = 100) String displayName
) {
}
