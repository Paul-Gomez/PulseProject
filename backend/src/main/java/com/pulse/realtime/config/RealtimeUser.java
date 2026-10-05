package com.pulse.realtime.config;

import com.pulse.common.security.PulseUserDetails;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.UUID;

public final class RealtimeUser {

    private RealtimeUser() {
    }

    public static UUID idOf(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof PulseUserDetails user) {
            return user.getId();
        }
        throw new MessageDeliveryException("Not authenticated");
    }
}
