package com.jobcupid.job_cupid.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)  // enables @PreAuthorize, @PostAuthorize, and @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter  jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Stateless API — no CSRF, no session
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints — public
                        .requestMatchers("/auth/**").permitAll()
                        // Stripe webhook — verified by signature, not JWT
                        .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/webhook").permitAll()
                        // Public plan listing
                        .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/plans").permitAll()
                        // Actuator health — public for K8s probes
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // JWT filter runs before Spring Security's username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Disable Spring Security's default form login and HTTP Basic
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7: UserDetailsService is a constructor argument, not a setter
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a bean so it can be injected if needed.
     * AuthService uses direct repository + BCrypt checks, so this is mainly for
     * future use (e.g. integrating OAuth2 or other auth mechanisms).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
