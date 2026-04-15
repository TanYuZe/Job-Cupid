package com.jobcupid.job_cupid.auth.dto;

import java.util.UUID;

import com.jobcupid.job_cupid.user.entity.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String   accessToken;
    private final String   refreshToken;
    private final String   tokenType;   // always "Bearer"
    private final long     expiresIn;   // access token TTL in seconds
    private final UUID     userId;
    private final String   email;
    private final UserRole role;
    private final boolean  premium;
}
