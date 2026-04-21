package com.jobcupid.job_cupid.job.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.ExperienceLevel;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobResponse {

    private UUID            id;
    private UUID            employerId;
    private String          title;
    private String          description;
    private String          category;
    private String          location;
    private Boolean         isRemote;
    private Integer         salaryMin;
    private Integer         salaryMax;
    private String          currency;
    private EmploymentType  employmentType;
    private ExperienceLevel experienceLevel;
    private List<String>    requiredSkills;
    private JobStatus       status;
    private Integer         boostScore;
    private Integer         applicationCount;
    private OffsetDateTime  expiresAt;
    private OffsetDateTime  createdAt;
    private OffsetDateTime  updatedAt;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .employerId(job.getEmployerId())
                .title(job.getTitle())
                .description(job.getDescription())
                .category(job.getCategory())
                .location(job.getLocation())
                .isRemote(job.getIsRemote())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .employmentType(job.getEmploymentType())
                .experienceLevel(job.getExperienceLevel())
                .requiredSkills(job.getRequiredSkills())
                .status(job.getStatus())
                .boostScore(job.getBoostScore())
                .applicationCount(job.getApplicationCount())
                .expiresAt(job.getExpiresAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
