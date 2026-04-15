package com.jobcupid.job_cupid.shared.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.jobcupid.job_cupid.user.entity.User;

/**
 * Spring Security principal wrapping the User entity.
 * Authorities are derived from the role field plus isPremium flag —
 * ROLE_PREMIUM is additive and never stored as a role value in the DB.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean isActive;
    private final boolean isBanned;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.id           = user.getId();
        this.email        = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.isActive     = Boolean.TRUE.equals(user.getIsActive());
        this.isBanned     = Boolean.TRUE.equals(user.getIsBanned());

        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        if (Boolean.TRUE.equals(user.getIsPremium())) {
            auths.add(new SimpleGrantedAuthority("ROLE_PREMIUM"));
        }
        this.authorities = List.copyOf(auths);
    }

    public static UserPrincipal of(User user) {
        return new UserPrincipal(user);
    }

    public UUID getId() {
        return id;
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return isActive && !isBanned;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isBanned;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
