package com.jobcupid.job_cupid.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.jobcupid.job_cupid.auth.config.JwtProperties;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.shared.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String DENY_PREFIX    = "token:deny:";
    private static final String REFRESH_PREFIX = "refresh_token:";

    private final JwtProperties    jwtProperties;
    private final StringRedisTemplate redis;

    // ── Access token ──────────────────────────────────────────────────────────

    /**
     * Generates a signed JWT access token for the given user.
     * Claims: sub=userId, email, role, premium, jti (unique per token).
     */
    public String generateAccessToken(User user) {
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenTtlSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti — used for blacklisting
                .subject(user.getId().toString())
                .claim("email",   user.getEmail())
                .claim("role",    user.getRole().name())
                .claim("premium", user.getIsPremium())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey())
                .compact();
    }

    /**
     * Validates and parses the JWT. Throws InvalidTokenException on any failure.
     */
    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Token has expired");
        } catch (JwtException ex) {
            throw new InvalidTokenException("Token is invalid");
        }
    }

    /**
     * Adds the access token's JTI to the Redis deny-list.
     * TTL matches the token's remaining lifetime so the key self-cleans.
     */
    public void blacklistAccessToken(String jti, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redis.opsForValue().set(
                    DENY_PREFIX + jti, "1", remainingSeconds, TimeUnit.SECONDS);
        }
    }

    /** Returns true if the given JTI has been blacklisted (logged out). */
    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(DENY_PREFIX + jti));
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    /**
     * Generates an opaque refresh token (random UUID), stores its SHA-256 hash
     * in Redis keyed by hash → userId, and returns the raw token to the caller.
     */
    public String generateAndStoreRefreshToken(UUID userId) {
        String raw  = UUID.randomUUID().toString();
        String hash = sha256(raw);
        Duration ttl = Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds());

        redis.opsForValue().set(
                REFRESH_PREFIX + hash, userId.toString(),
                ttl.toSeconds(), TimeUnit.SECONDS);

        return raw;
    }

    /**
     * Validates the raw refresh token.
     * Returns the owner's userId string, or throws InvalidTokenException if unknown/expired.
     */
    public String validateRefreshToken(String raw) {
        String hash   = sha256(raw);
        String userId = redis.opsForValue().get(REFRESH_PREFIX + hash);
        if (userId == null) {
            throw new InvalidTokenException("Refresh token is invalid or has expired");
        }
        return userId;
    }

    /**
     * Rotates the refresh token: deletes the old hash, stores the new one.
     * Returns the new raw token.
     */
    public String rotateRefreshToken(String oldRaw, UUID userId) {
        redis.delete(REFRESH_PREFIX + sha256(oldRaw));
        return generateAndStoreRefreshToken(userId);
    }

    /** Revokes the refresh token — called on logout. */
    public void revokeRefreshToken(String raw) {
        redis.delete(REFRESH_PREFIX + sha256(raw));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SecretKey secretKey() {
        byte[] keyBytes = jwtProperties.getSecret()
                .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
