package com.pulse.messages.dto;

import java.util.List;
import java.util.UUID;

public record ReactionSummary(String emoji, int count, List<UUID> userIds) {
}
