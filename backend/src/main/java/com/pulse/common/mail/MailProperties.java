package com.pulse.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulse.mail")
public record MailProperties(boolean enabled, String from, String frontendUrl) {
}
