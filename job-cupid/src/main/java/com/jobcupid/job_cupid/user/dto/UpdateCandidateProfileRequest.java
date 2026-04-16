package com.jobcupid.job_cupid.user.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCandidateProfileRequest {

    @Size(max = 255, message = "Headline must not exceed 255 characters")
    private String headline;

    @Size(max = 500, message = "Resume URL must not exceed 500 characters")
    private String resumeUrl;

    private List<String> skills;

    @Min(value = 0, message = "Years of experience must be 0 or greater")
    private Short yearsOfExperience;

    @Min(value = 0, message = "Minimum salary must be 0 or greater")
    private Integer desiredSalaryMin;

    @Min(value = 0, message = "Maximum salary must be 0 or greater")
    private Integer desiredSalaryMax;

    private Boolean preferredRemote;

    @Size(max = 255, message = "Preferred location must not exceed 255 characters")
    private String preferredLocation;

    private Boolean isOpenToWork;
}
