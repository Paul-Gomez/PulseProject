package com.pulse.identity.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.common.security.JwtService;
import com.pulse.identity.dto.LoginRequest;
import com.pulse.identity.dto.RegisterRequest;
import com.pulse.identity.dto.TokenPairResponse;
import com.pulse.identity.entity.RefreshToken;
import com.pulse.identity.repository.RefreshTokenRepository;
import com.pulse.users.entity.User;
import com.pulse.users.entity.UserStatus;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenHasher refreshTokenHasher;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public TokenPairResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_IN_USE, "Email already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.conflict(ErrorCode.USERNAME_ALREADY_IN_USE, "Username already in use");
        }

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .status(UserStatus.OFFLINE)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        loginAttemptService.requireNotLocked(request.email());

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Unknown emails are counted too, so the lock cannot be used to find out which emails exist.
            loginAttemptService.recordFailure(request.email());
            throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }

        loginAttemptService.reset(request.email());
        return issueTokenPair(user);
    }

    @Transactional
    public TokenPairResponse refresh(String rawRefreshToken) {
        String hash = refreshTokenHasher.hash(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));

        if (!storedToken.isActive()) {
            // Reuse of an already-revoked token is treated as a compromise signal: revoke every active
            // refresh token for that user so a stolen token cannot keep issuing new sessions.
            if (storedToken.getRevokedAt() != null) {
                revokeAllForUser(storedToken.getUserId());
            }
            throw ApiException.unauthorized(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token");
        }

        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = refreshTokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    private void revokeAllForUser(UUID userId) {
        var activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        Instant now = Instant.now();
        activeTokens.forEach(t -> t.setRevokedAt(now));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private TokenPairResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(jwtService.refreshTokenTtlDays(), ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return TokenPairResponse.bearer(accessToken, rawRefreshToken);
    }
}
