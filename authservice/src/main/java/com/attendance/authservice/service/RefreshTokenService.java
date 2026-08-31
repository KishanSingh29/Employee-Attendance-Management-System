package com.attendance.authservice.service;

import com.attendance.authservice.entity.RefreshToken;
import com.attendance.authservice.entity.User;
import com.attendance.authservice.exception.InvalidTokenException;
import com.attendance.authservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns the lifecycle of opaque refresh tokens persisted in {@code refresh_tokens}.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlMillis;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh.expiration}") long refreshTokenTtlMillis) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlMillis = refreshTokenTtlMillis;
    }

    @Transactional
    public RefreshToken issue(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenTtlMillis))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (!refreshToken.isActive()) {
            throw new InvalidTokenException("Refresh token has expired or been revoked");
        }
        return refreshToken;
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }
}
