package com.jobcupid.job_cupid.user.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unified PUT /api/v1/users/me request body.
 * All fields are optional (PATCH semantics). The service applies only non-null values.
 * Candidate-specific fields are ignored for EMPLOYER users and vice versa.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    // ── Shared (User entity) ──────────────────────────────────────────────────
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    private String bio;

    private String location;

    @Size(max = 500)
    private String photoUrl;

    // ── Candidate-specific ────────────────────────────────────────────────────
    @Size(max = 255)
    private String headline;

    @Size(max = 500)
    private String resumeUrl;

    private List<String> skills;

    @Min(0)
    private Short yearsOfExperience;

    @Min(0)
    private Integer desiredSalaryMin;

    @Min(0)
    private Integer desiredSalaryMax;

    private Boolean preferredRemote;

    @Size(max = 255)
    private String preferredLocation;

    private Boolean isOpenToWork;

    // ── Employer-specific ─────────────────────────────────────────────────────
    @Size(max = 255)
    private String companyName;

    private String companyDescription;

    @Size(max = 500)
    private String companyWebsite;

    @Size(max = 500)
    private String companyLogoUrl;

    @Pattern(regexp = "^(1-10|11-50|51-200|201-500|501-1000|1000\\+)$",
             message = "companySize must be one of: 1-10, 11-50, 51-200, 201-500, 501-1000, 1000+")
    private String companySize;

    @Size(max = 100)
    private String industry;

    private Short foundedYear;
}
