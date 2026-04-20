package com.jobcupid.job_cupid.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jobcupid.job_cupid.auth.config.JwtProperties;
import com.jobcupid.job_cupid.auth.dto.LoginRequest;
import com.jobcupid.job_cupid.shared.exception.InvalidCredentialsException;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository  userRepository;
    @Mock TokenService    tokenService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProperties   jwtProperties;

    @InjectMocks AuthService authService;

    @Test
    void login_throwsInvalidCredentials_whenUserIsSoftDeleted() {
        LoginRequest request = new LoginRequest();
        request.setEmail("deleted@example.com");
        request.setPassword("anyPassword");

        when(userRepository.findByEmailAndDeletedAtIsNull("deleted@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
