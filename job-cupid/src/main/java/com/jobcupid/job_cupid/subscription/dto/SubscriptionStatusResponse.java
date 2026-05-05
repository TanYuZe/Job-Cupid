package com.jobcupid.job_cupid.subscription.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionStatusResponse {

    private boolean active;
    private String  plan;
    private String  status;
    private Instant currentPeriodEnd;
}
