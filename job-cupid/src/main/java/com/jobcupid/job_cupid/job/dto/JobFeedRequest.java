package com.jobcupid.job_cupid.job.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobFeedRequest {

    private String  category;
    private String  location;
    private Boolean remote;

    @Min(0)
    private Integer salaryMin;

    @Min(0)
    private Integer salaryMax;

    private String keyword;
    private String cursor;

    @Min(1)
    @Max(50)
    private int size = 20;
}
