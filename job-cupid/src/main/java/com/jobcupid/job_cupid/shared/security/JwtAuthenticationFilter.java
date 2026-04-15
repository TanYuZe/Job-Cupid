package com.jobcupid.job_cupid.shared.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.shared.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs once per request. Extracts the Bearer token, validates it, checks the
 * deny-list, and populates the SecurityContext — all without hitting the DB.
 * All user data needed for authorization lives in the JWT claims.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService            tokenService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = tokenService.parseAccessToken(token);

                String jti = claims.getId();
                if (tokenService.isAccessTokenBlacklisted(jti)) {
                    log.debug("Rejected blacklisted token jti={}", jti);
                } else {
                    String email = claims.get("email", String.class);

                    // Load UserPrincipal from DB only if email claim is present.
                    // This is the single DB hit per request — cached in SecurityContext for the duration.
                    var userDetails = userDetailsService.loadUserByUsername(email);

                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (InvalidTokenException ex) {
                log.debug("Invalid JWT token: {}", ex.getMessage());
                // Do not set authentication — Spring Security will return 401
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
