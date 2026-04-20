package com.jobcupid.job_cupid.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobcupid.job_cupid.user.dto.CandidateProfileResponse;
import com.jobcupid.job_cupid.user.dto.EmployerProfileResponse;
import com.jobcupid.job_cupid.user.dto.UpdateCandidateProfileRequest;
import com.jobcupid.job_cupid.user.dto.UpdateProfileRequest;
import com.jobcupid.job_cupid.user.entity.CandidateProfile;
import com.jobcupid.job_cupid.user.entity.EmployerProfile;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.CandidateProfileRepository;
import com.jobcupid.job_cupid.user.repository.EmployerProfileRepository;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository             userRepository;
    @Mock CandidateProfileRepository candidateProfileRepository;
    @Mock EmployerProfileRepository  employerProfileRepository;
    @Mock CandidateProfileService    candidateProfileService;
    @Mock EmployerProfileService     employerProfileService;

    @InjectMocks UserService userService;

    // ── getCurrentUser ────────────────────────────────────────────────────────

    @Test
    void getCurrentUser_returnsCandidateResponse_forUserRole() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, UserRole.USER);
        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId).headline("Java Dev").skills(List.of("Java")).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Object result = userService.getCurrentUser(userId);

        assertThat(result).isInstanceOf(CandidateProfileResponse.class);
        CandidateProfileResponse response = (CandidateProfileResponse) result;
        assertThat(response.getHeadline()).isEqualTo("Java Dev");
        assertThat(response.getSkills()).containsExactly("Java");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getCurrentUser_returnsEmployerResponse_forEmployerRole() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, UserRole.EMPLOYER);
        EmployerProfile profile = EmployerProfile.builder()
                .userId(userId).companyName("Acme Corp").industry("Tech").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(employerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Object result = userService.getCurrentUser(userId);

        assertThat(result).isInstanceOf(EmployerProfileResponse.class);
        EmployerProfileResponse response = (EmployerProfileResponse) result;
        assertThat(response.getCompanyName()).isEqualTo("Acme Corp");
    }

    @Test
    void getCurrentUser_returnsResponseWithNullProfile_whenProfileNotYetCreated() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Object result = userService.getCurrentUser(userId);

        assertThat(result).isInstanceOf(CandidateProfileResponse.class);
        CandidateProfileResponse response = (CandidateProfileResponse) result;
        assertThat(response.getHeadline()).isNull();
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesUserFields_andDelegatesToCandidateService() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, UserRole.USER);
        CandidateProfile updatedProfile = CandidateProfile.builder()
                .userId(userId).headline("Senior Dev").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(candidateProfileService.createOrUpdate(eq(userId), any(UpdateCandidateProfileRequest.class)))
                .thenReturn(updatedProfile);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Alice Updated");
        request.setHeadline("Senior Dev");

        userService.updateProfile(userId, request);

        verify(userRepository).save(user);
        assertThat(user.getFirstName()).isEqualTo("Alice Updated");
        verify(candidateProfileService).createOrUpdate(eq(userId), any());
    }

    @Test
    void updateProfile_onlyNonNullFieldsApplied_existingValuesPreserved() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, UserRole.USER);
        user.setLocation("Singapore");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(candidateProfileService.createOrUpdate(eq(userId), any()))
                .thenReturn(CandidateProfile.builder().userId(userId).build());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("NewName");
        // location is null — should not be overwritten

        userService.updateProfile(userId, request);

        assertThat(user.getFirstName()).isEqualTo("NewName");
        assertThat(user.getLocation()).isEqualTo("Singapore");  // preserved
    }

    @Test
    void softDeleteUser_setsIsActiveFalse_andDeletedAtNotNull() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.softDeleteUser(userId);

        assertThat(user.getIsActive()).isFalse();
        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User userWithRole(UUID id, UserRole role) {
        return User.builder()
                .id(id)
                .email("alice@example.com")
                .passwordHash("$2a$12$x")
                .firstName("Alice")
                .lastName("Chen")
                .role(role)
                .isPremium(false)
                .isActive(true)
                .isBanned(false)
                .build();
    }
}
