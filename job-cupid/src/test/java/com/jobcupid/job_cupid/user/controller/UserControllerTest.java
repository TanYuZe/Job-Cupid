package com.jobcupid.job_cupid.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.JwtAuthenticationFilter;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.dto.CandidateProfileResponse;
import com.jobcupid.job_cupid.user.dto.EmployerProfileResponse;
import com.jobcupid.job_cupid.user.dto.PhotoConfirmRequest;
import com.jobcupid.job_cupid.user.dto.PhotoUploadRequest;
import com.jobcupid.job_cupid.user.dto.PhotoUploadResponse;
import com.jobcupid.job_cupid.user.dto.UpdateProfileRequest;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.service.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    @MockitoBean UserService              userService;
    @MockitoBean JwtAuthenticationFilter  jwtAuthenticationFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean TokenService             tokenService;

    private UUID candidateId;
    private UUID employerId;
    private UsernamePasswordAuthenticationToken candidateAuth;
    private UsernamePasswordAuthenticationToken employerAuth;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        employerId  = UUID.randomUUID();

        UserPrincipal candidatePrincipal = UserPrincipal.of(User.builder()
                .id(candidateId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(false).isActive(true).isBanned(false)
                .build());

        UserPrincipal employerPrincipal = UserPrincipal.of(User.builder()
                .id(employerId).email("bob@acme.com")
                .passwordHash("x").firstName("Bob").lastName("Smith")
                .role(UserRole.EMPLOYER).isPremium(false).isActive(true).isBanned(false)
                .build());

        candidateAuth = new UsernamePasswordAuthenticationToken(
                candidatePrincipal, null, candidatePrincipal.getAuthorities());
        employerAuth = new UsernamePasswordAuthenticationToken(
                employerPrincipal, null, employerPrincipal.getAuthorities());
    }

    @Test
    void getMe_returnsCandidateResponse_forUserRole() throws Exception {
        CandidateProfileResponse response = CandidateProfileResponse.builder()
                .userId(candidateId).email("alice@example.com")
                .firstName("Alice").lastName("Chen")
                .role(UserRole.USER).headline("Java Dev")
                .skills(List.of("Java", "Spring")).build();

        when(userService.getCurrentUser(candidateId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.headline").value("Java Dev"))
                .andExpect(jsonPath("$.skills[0]").value("Java"));
    }

    @Test
    void getMe_returnsEmployerResponse_forEmployerRole() throws Exception {
        EmployerProfileResponse response = EmployerProfileResponse.builder()
                .userId(employerId).email("bob@acme.com")
                .firstName("Bob").lastName("Smith")
                .role(UserRole.EMPLOYER).companyName("Acme Corp")
                .industry("Technology").build();

        when(userService.getCurrentUser(employerId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@acme.com"))
                .andExpect(jsonPath("$.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.industry").value("Technology"));
    }

    @Test
    void getMe_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMe_returnsUpdatedResponse_withNewHeadline() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setHeadline("Senior Engineer");

        CandidateProfileResponse response = CandidateProfileResponse.builder()
                .userId(candidateId).email("alice@example.com")
                .role(UserRole.USER).headline("Senior Engineer").build();

        when(userService.updateProfile(eq(candidateId), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Senior Engineer"));
    }

    @Test
    void deleteMe_returns204_whenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());

        verify(userService).softDeleteUser(candidateId);
    }

    @Test
    void requestPhotoUpload_returns200_withPresignedUrl() throws Exception {
        PhotoUploadRequest request = new PhotoUploadRequest();
        request.setContentType("image/jpeg");

        PhotoUploadResponse response = PhotoUploadResponse.builder()
                .presignedUrl("https://test-bucket.s3.amazonaws.com/photos/key?sig=abc")
                .publicUrl("https://test-bucket.s3.amazonaws.com/photos/key")
                .expiresIn(300)
                .build();

        when(userService.generatePhotoUploadUrl(eq(candidateId), eq("image/jpeg"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/me/photo")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedUrl").isNotEmpty())
                .andExpect(jsonPath("$.publicUrl").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(300));
    }

    @Test
    void confirmPhotoUpload_returns200_withUpdatedPhotoUrl() throws Exception {
        PhotoConfirmRequest request = new PhotoConfirmRequest();
        request.setPublicUrl("https://test-bucket.s3.amazonaws.com/photos/key");

        CandidateProfileResponse response = CandidateProfileResponse.builder()
                .userId(candidateId).email("alice@example.com")
                .role(UserRole.USER)
                .photoUrl("https://test-bucket.s3.amazonaws.com/photos/key")
                .build();

        when(userService.confirmPhoto(eq(candidateId), any(PhotoConfirmRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me/photo/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://test-bucket.s3.amazonaws.com/photos/key"));
    }
}
