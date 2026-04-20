package com.jobcupid.job_cupid.user.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.user.dto.CandidateProfileResponse;
import com.jobcupid.job_cupid.user.dto.EmployerProfileResponse;
import com.jobcupid.job_cupid.user.dto.UpdateCandidateProfileRequest;
import com.jobcupid.job_cupid.user.dto.UpdateEmployerProfileRequest;
import com.jobcupid.job_cupid.user.dto.UpdateProfileRequest;
import com.jobcupid.job_cupid.user.entity.CandidateProfile;
import com.jobcupid.job_cupid.user.entity.EmployerProfile;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.CandidateProfileRepository;
import com.jobcupid.job_cupid.user.repository.EmployerProfileRepository;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository              userRepository;
    private final CandidateProfileRepository  candidateProfileRepository;
    private final EmployerProfileRepository   employerProfileRepository;
    private final CandidateProfileService     candidateProfileService;
    private final EmployerProfileService      employerProfileService;

    /**
     * Returns a role-specific response combining User + Profile data.
     */
    @Transactional(readOnly = true)
    public Object getCurrentUser(UUID userId) {
        User user = loadUser(userId);

        if (user.getRole() == UserRole.EMPLOYER) {
            EmployerProfile profile = employerProfileRepository.findByUserId(userId).orElse(null);
            return EmployerProfileResponse.from(user, profile);
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId).orElse(null);
        return CandidateProfileResponse.from(user, profile);
    }

    /**
     * Applies shared User fields, then delegates profile-specific fields by role.
     * Returns updated role-specific response.
     */
    @Transactional
    public Object updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = loadUser(userId);

        applyUserFields(user, request);
        userRepository.save(user);

        if (user.getRole() == UserRole.EMPLOYER) {
            EmployerProfile profile = employerProfileService.createOrUpdate(userId, toEmployerRequest(request));
            return EmployerProfileResponse.from(user, profile);
        }

        CandidateProfile profile = candidateProfileService.createOrUpdate(userId, toCandidateRequest(request));
        return CandidateProfileResponse.from(user, profile);
    }

    // ── Legacy method kept for backward compatibility ─────────────────────────

    public User createUser(User user) {
        userRepository.save(user);
        return user;
    }

    @Transactional
    public void softDeleteUser(UUID userId) {
        User user = loadUser(userId);
        user.setIsActive(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void applyUserFields(User user, UpdateProfileRequest req) {
        if (req.getFirstName()  != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()   != null) user.setLastName(req.getLastName());
        if (req.getBio()        != null) user.setBio(req.getBio());
        if (req.getLocation()   != null) user.setLocation(req.getLocation());
        if (req.getPhotoUrl()   != null) user.setPhotoUrl(req.getPhotoUrl());
    }

    private UpdateCandidateProfileRequest toCandidateRequest(UpdateProfileRequest req) {
        UpdateCandidateProfileRequest r = new UpdateCandidateProfileRequest();
        r.setHeadline(req.getHeadline());
        r.setResumeUrl(req.getResumeUrl());
        r.setSkills(req.getSkills());
        r.setYearsOfExperience(req.getYearsOfExperience());
        r.setDesiredSalaryMin(req.getDesiredSalaryMin());
        r.setDesiredSalaryMax(req.getDesiredSalaryMax());
        r.setPreferredRemote(req.getPreferredRemote());
        r.setPreferredLocation(req.getPreferredLocation());
        r.setIsOpenToWork(req.getIsOpenToWork());
        return r;
    }

    private UpdateEmployerProfileRequest toEmployerRequest(UpdateProfileRequest req) {
        UpdateEmployerProfileRequest r = new UpdateEmployerProfileRequest();
        r.setCompanyName(req.getCompanyName());
        r.setCompanyDescription(req.getCompanyDescription());
        r.setCompanyWebsite(req.getCompanyWebsite());
        r.setCompanyLogoUrl(req.getCompanyLogoUrl());
        r.setCompanySize(req.getCompanySize());
        r.setIndustry(req.getIndustry());
        r.setFoundedYear(req.getFoundedYear());
        return r;
    }
}
