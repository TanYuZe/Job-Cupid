package com.jobcupid.job_cupid.shared.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Loads users by email for Spring Security's DaoAuthenticationProvider.
 * Excludes soft-deleted accounts via the repository query.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return UserPrincipal.of(user);
    }
}
