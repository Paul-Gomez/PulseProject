package com.pulse.identity.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.identity.entity.AccountToken;
import com.pulse.identity.entity.AccountTokenType;
import com.pulse.identity.repository.AccountTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Single-use, expiring tokens for email verification and password reset. Only their hash is stored. */
@Service
@RequiredArgsConstructor
public class AccountTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountTokenRepository tokenRepository;
    private final RefreshTokenHasher hasher;

    /** Issuing a new token invalidates any earlier unused one of the same type for that user. */
    @Transactional
    public String issue(UUID userId, AccountTokenType type, Duration lifetime) {
        tokenRepository.deleteUnused(userId, type);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(AccountToken.builder()
                .userId(userId)
                .type(type)
                .tokenHash(hasher.hash(rawToken))
                .expiresAt(Instant.now().plus(lifetime))
                .build());
        return rawToken;
    }

    /** @return the id of the user the token belongs to */
    @Transactional
    public UUID consume(String rawToken, AccountTokenType type) {
        AccountToken token = tokenRepository.findByTokenHashAndType(hasher.hash(rawToken), type)
                .orElseThrow(this::invalidToken);

        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)
                || tokenRepository.markUsed(token.getId(), now) == 0) {
            throw invalidToken();
        }
        return token.getUserId();
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TOKEN, "The link is invalid or has expired");
    }
}
