package com.pulse.files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulse.storage.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey, String bucket) {
}
