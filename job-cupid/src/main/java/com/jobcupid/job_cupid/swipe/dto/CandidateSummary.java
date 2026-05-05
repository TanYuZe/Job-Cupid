package com.jobcupid.job_cupid.swipe.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CandidateSummary {
    private final UUID candidateId;
}
