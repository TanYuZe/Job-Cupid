package com.jobcupid.job_cupid.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {
    private final UUID   id;
    private final UUID   candidateId;
    private final UUID   jobId;
    private final String coverLetter;
    private final String resumeUrl;
    private final String status;
    private final Instant appliedAt;
}
