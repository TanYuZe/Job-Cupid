package com.jobcupid.job_cupid.job.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.ExperienceLevel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateJobRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 100)
    private String category;

    @Size(max = 255)
    private String location;

    private Boolean isRemote;

    @Min(0)
    private Integer salaryMin;

    @Min(0)
    private Integer salaryMax;

    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code")
    private String currency;

    @NotNull
    private EmploymentType employmentType;

    private ExperienceLevel experienceLevel;

    private List<String> requiredSkills;

    private OffsetDateTime expiresAt;
}
