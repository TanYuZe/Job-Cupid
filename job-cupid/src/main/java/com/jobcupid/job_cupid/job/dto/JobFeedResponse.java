package com.jobcupid.job_cupid.job.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobFeedResponse {

    private List<JobResponse> items;
    private String            nextCursor;
    private boolean           hasMore;
}
