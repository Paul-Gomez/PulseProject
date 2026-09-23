package com.pulse.common.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtProperties properties = new JwtProperties(
            "unit-test-secret-key-with-enough-length-1234567890", 15, 30);
    private final JwtService jwtService = new JwtService(properties);

    @Test
    void generateAccessToken_thenExtractUserId_roundTripsCorrectly() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "user@example.com");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");

        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }
}
