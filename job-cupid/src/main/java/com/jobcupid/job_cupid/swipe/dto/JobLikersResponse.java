package com.jobcupid.job_cupid.swipe.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobLikersResponse {
    private final UUID jobId;
    private final List<CandidateSummary> likers;
}
