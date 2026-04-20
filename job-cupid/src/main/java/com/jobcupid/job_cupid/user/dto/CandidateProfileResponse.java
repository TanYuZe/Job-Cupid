package com.jobcupid.job_cupid.user.dto;

import java.util.List;
import java.util.UUID;

import com.jobcupid.job_cupid.user.entity.CandidateProfile;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CandidateProfileResponse {

    // ── User fields ───────────────────────────────────────────────────────────
    private UUID   userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private boolean premium;
    private String bio;
    private String location;
    private String photoUrl;

    // ── Candidate profile fields ──────────────────────────────────────────────
    private String       headline;
    private String       resumeUrl;
    private List<String> skills;
    private Short        yearsOfExperience;
    private Integer      desiredSalaryMin;
    private Integer      desiredSalaryMax;
    private boolean      preferredRemote;
    private String       preferredLocation;
    private boolean      isOpenToWork;

    public static CandidateProfileResponse from(User user, CandidateProfile profile) {
        CandidateProfileResponseBuilder builder = CandidateProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .premium(Boolean.TRUE.equals(user.getIsPremium()))
                .bio(user.getBio())
                .location(user.getLocation())
                .photoUrl(user.getPhotoUrl());

        if (profile != null) {
            builder
                .headline(profile.getHeadline())
                .resumeUrl(profile.getResumeUrl())
                .skills(profile.getSkills())
                .yearsOfExperience(profile.getYearsOfExperience())
                .desiredSalaryMin(profile.getDesiredSalaryMin())
                .desiredSalaryMax(profile.getDesiredSalaryMax())
                .preferredRemote(Boolean.TRUE.equals(profile.getPreferredRemote()))
                .preferredLocation(profile.getPreferredLocation())
                .isOpenToWork(Boolean.TRUE.equals(profile.getIsOpenToWork()));
        }
        return builder.build();
    }
}
