package com.pulse.workspaces.service;

import java.util.Map;

/** Who can act on whom: you can only manage members whose role is strictly below yours. */
public final class RoleHierarchy {

    private static final Map<String, Integer> RANKS = Map.of(
            "OWNER", 4,
            "ADMIN", 3,
            "MODERATOR", 2,
            "MEMBER", 1,
            "GUEST", 0);

    private RoleHierarchy() {
    }

    public static int rankOf(String roleName) {
        return RANKS.getOrDefault(roleName, -1);
    }

    public static boolean outranks(String actorRole, String targetRole) {
        return rankOf(actorRole) > rankOf(targetRole);
    }
}
