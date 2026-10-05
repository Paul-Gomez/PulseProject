package com.pulse.workspaces.dto;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String iconUrl
) {
}
