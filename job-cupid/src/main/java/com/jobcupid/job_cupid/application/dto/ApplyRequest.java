package com.jobcupid.job_cupid.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyRequest {
    private String coverLetter;
    private String resumeUrl;
}
