package com.jobcupid.job_cupid.match.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchResponse {

    private UUID    id;
    private UUID    candidateId;
    private UUID    employerId;
    private UUID    jobId;
    private UUID    applicationId;
    private String  status;
    private Instant matchedAt;
}
