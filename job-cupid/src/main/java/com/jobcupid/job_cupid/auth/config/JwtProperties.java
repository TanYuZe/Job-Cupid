package com.jobcupid.job_cupid.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Binds jwt.* properties from application.yml.
 * secret      — raw string, at least 32 chars (256 bits) for HMAC-SHA256
 * access-token-ttl-seconds  — default 900  (15 min)
 * refresh-token-ttl-seconds — default 604800 (7 days)
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessTokenTtlSeconds = 900;
    private long refreshTokenTtlSeconds = 604800;
}
