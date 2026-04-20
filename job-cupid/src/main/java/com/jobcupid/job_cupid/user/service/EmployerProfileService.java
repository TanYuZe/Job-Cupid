package com.jobcupid.job_cupid.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.user.dto.UpdateEmployerProfileRequest;
import com.jobcupid.job_cupid.user.entity.EmployerProfile;
import com.jobcupid.job_cupid.user.repository.EmployerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final EmployerProfileRepository employerProfileRepository;

    @Transactional(readOnly = true)
    public EmployerProfile getByUserId(UUID userId) {
        return employerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employer profile not found for user: " + userId));
    }

    /**
     * Upsert — creates profile on first call, updates on subsequent calls.
     * companyName is required on creation; null skips update on existing profiles.
     */
    @Transactional
    public EmployerProfile createOrUpdate(UUID userId, UpdateEmployerProfileRequest request) {
        EmployerProfile profile = employerProfileRepository
                .findByUserId(userId)
                .orElseGet(() -> EmployerProfile.builder()
                        .userId(userId)
                        .companyName(request.getCompanyName() != null ? request.getCompanyName() : "")
                        .build());

        applyUpdates(profile, request);
        return employerProfileRepository.save(profile);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyUpdates(EmployerProfile profile, UpdateEmployerProfileRequest request) {
        if (request.getCompanyName()        != null) profile.setCompanyName(request.getCompanyName());
        if (request.getCompanyDescription() != null) profile.setCompanyDescription(request.getCompanyDescription());
        if (request.getCompanyWebsite()     != null) profile.setCompanyWebsite(request.getCompanyWebsite());
        if (request.getCompanyLogoUrl()     != null) profile.setCompanyLogoUrl(request.getCompanyLogoUrl());
        if (request.getCompanySize()        != null) profile.setCompanySize(request.getCompanySize());
        if (request.getIndustry()           != null) profile.setIndustry(request.getIndustry());
        if (request.getFoundedYear()        != null) profile.setFoundedYear(request.getFoundedYear());
    }
}
