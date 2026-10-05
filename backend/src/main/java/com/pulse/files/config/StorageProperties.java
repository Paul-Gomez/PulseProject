package com.pulse.files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulse.storage")
public record StorageProperties(long maxFileSizeBytes) {
}
