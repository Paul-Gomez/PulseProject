package com.pulse;

import com.pulse.common.ratelimit.RateLimitProperties;
import com.pulse.files.config.MinioProperties;
import com.pulse.files.config.StorageProperties;
import com.pulse.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class, MinioProperties.class, StorageProperties.class})
public class PulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseApplication.class, args);
    }
}
