package com.jobcupid.job_cupid.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.user.dto.UpdateCandidateProfileRequest;
import com.jobcupid.job_cupid.user.entity.CandidateProfile;
import com.jobcupid.job_cupid.user.repository.CandidateProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository candidateProfileRepository;

    /**
     * Returns the profile for a given user, or throws if not yet created.
     */
    @Transactional(readOnly = true)
    public CandidateProfile getByUserId(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate profile not found for user: " + userId));
    }

    /**
     * Upsert: creates the profile on first call, updates it on subsequent calls.
     * Only non-null fields in the request are applied (partial update semantics).
     */
    @Transactional
    public CandidateProfile createOrUpdate(UUID userId, UpdateCandidateProfileRequest request) {
        validateSalaryRange(request);

        CandidateProfile profile = candidateProfileRepository
                .findByUserId(userId)
                .orElseGet(() -> CandidateProfile.builder().userId(userId).build());

        applyUpdates(profile, request);
        return candidateProfileRepository.save(profile);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateSalaryRange(UpdateCandidateProfileRequest request) {
        Integer min = request.getDesiredSalaryMin();
        Integer max = request.getDesiredSalaryMax();
        if (min != null && max != null && min > max) {
            throw new BusinessRuleException(
                    "desiredSalaryMin (" + min + ") must not exceed desiredSalaryMax (" + max + ")");
        }
    }

    private void applyUpdates(CandidateProfile profile, UpdateCandidateProfileRequest request) {
        if (request.getHeadline()          != null) profile.setHeadline(request.getHeadline());
        if (request.getResumeUrl()         != null) profile.setResumeUrl(request.getResumeUrl());
        if (request.getSkills()            != null) profile.setSkills(request.getSkills());
        if (request.getYearsOfExperience() != null) profile.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getDesiredSalaryMin()  != null) profile.setDesiredSalaryMin(request.getDesiredSalaryMin());
        if (request.getDesiredSalaryMax()  != null) profile.setDesiredSalaryMax(request.getDesiredSalaryMax());
        if (request.getPreferredRemote()   != null) profile.setPreferredRemote(request.getPreferredRemote());
        if (request.getPreferredLocation() != null) profile.setPreferredLocation(request.getPreferredLocation());
        if (request.getIsOpenToWork()      != null) profile.setIsOpenToWork(request.getIsOpenToWork());
    }
}
