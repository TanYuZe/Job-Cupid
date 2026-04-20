package com.jobcupid.job_cupid.user.dto;

import java.util.UUID;

import com.jobcupid.job_cupid.user.entity.EmployerProfile;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployerProfileResponse {

    // ── User fields ───────────────────────────────────────────────────────────
    private UUID     userId;
    private String   email;
    private String   firstName;
    private String   lastName;
    private UserRole role;
    private boolean  premium;
    private String   photoUrl;

    // ── Employer profile fields ───────────────────────────────────────────────
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyLogoUrl;
    private String companySize;
    private String industry;
    private Short  foundedYear;

    public static EmployerProfileResponse from(User user, EmployerProfile profile) {
        EmployerProfileResponseBuilder builder = EmployerProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .premium(Boolean.TRUE.equals(user.getIsPremium()))
                .photoUrl(user.getPhotoUrl());

        if (profile != null) {
            builder
                .companyName(profile.getCompanyName())
                .companyDescription(profile.getCompanyDescription())
                .companyWebsite(profile.getCompanyWebsite())
                .companyLogoUrl(profile.getCompanyLogoUrl())
                .companySize(profile.getCompanySize())
                .industry(profile.getIndustry())
                .foundedYear(profile.getFoundedYear());
        }
        return builder.build();
    }
}
