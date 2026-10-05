package com.pulse.workspaces.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequest(
        @NotBlank String roleName
) {
}
