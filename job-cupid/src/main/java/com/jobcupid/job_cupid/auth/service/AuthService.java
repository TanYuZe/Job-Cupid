package com.jobcupid.job_cupid.auth.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.auth.config.JwtProperties;
import com.jobcupid.job_cupid.auth.dto.AuthResponse;
import com.jobcupid.job_cupid.auth.dto.LoginRequest;
import com.jobcupid.job_cupid.auth.dto.RegisterRequest;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.DuplicateEmailException;
import com.jobcupid.job_cupid.shared.exception.InvalidCredentialsException;
import com.jobcupid.job_cupid.shared.exception.InvalidTokenException;
import com.jobcupid.job_cupid.shared.exception.UserBannedException;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final TokenService    tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties   jwtProperties;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new BusinessRuleException("Cannot self-register as ADMIN");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .isPremium(false)
                .isActive(true)
                .isBanned(false)
                .build();

        userRepository.save(user);
        log.info("New user registered: id={} role={}", user.getId(), user.getRole());

        return buildAuthResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();  // same exception — no user enumeration
        }

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new UserBannedException();
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User logged in: id={}", user.getId());
        return buildAuthResponse(user);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String userIdStr = tokenService.validateRefreshToken(rawRefreshToken);
        UUID userId = UUID.fromString(userIdStr);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User associated with token no longer exists"));

        if (Boolean.TRUE.equals(user.getIsBanned()) || !Boolean.TRUE.equals(user.getIsActive())) {
            tokenService.revokeRefreshToken(rawRefreshToken);
            throw new UserBannedException();
        }

        String newRefreshToken = tokenService.rotateRefreshToken(rawRefreshToken, userId);
        String newAccessToken  = tokenService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenTtlSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .premium(Boolean.TRUE.equals(user.getIsPremium()))
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    public void logout(String accessToken, String rawRefreshToken) {
        try {
            Claims claims = tokenService.parseAccessToken(accessToken);
            String jti = claims.getId();

            long expiresAt  = claims.getExpiration().getTime();
            long remaining  = (expiresAt - System.currentTimeMillis()) / 1000;

            tokenService.blacklistAccessToken(jti, remaining);
        } catch (InvalidTokenException ex) {
            // Access token already expired — blacklisting is a no-op; proceed to revoke refresh
            log.debug("Access token already expired during logout — skipping blacklist");
        }

        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            tokenService.revokeRefreshToken(rawRefreshToken);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateAndStoreRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenTtlSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .premium(Boolean.TRUE.equals(user.getIsPremium()))
                .build();
    }
}
