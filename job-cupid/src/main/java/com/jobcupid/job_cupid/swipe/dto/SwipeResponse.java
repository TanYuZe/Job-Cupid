package com.jobcupid.job_cupid.swipe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SwipeResponse {
    private String action;
    private boolean recorded;
}
