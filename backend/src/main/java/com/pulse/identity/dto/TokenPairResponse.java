package com.pulse.identity.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static TokenPairResponse bearer(String accessToken, String refreshToken) {
        return new TokenPairResponse(accessToken, refreshToken, "Bearer");
    }
}
