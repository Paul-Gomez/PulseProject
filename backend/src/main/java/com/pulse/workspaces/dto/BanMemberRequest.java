package com.pulse.workspaces.dto;

import jakarta.validation.constraints.Size;

public record BanMemberRequest(@Size(max = 255) String reason) {
}
