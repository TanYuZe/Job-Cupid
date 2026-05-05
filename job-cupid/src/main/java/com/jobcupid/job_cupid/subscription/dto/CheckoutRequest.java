package com.jobcupid.job_cupid.subscription.dto;

import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CheckoutRequest {

    @NotNull
    private SubscriptionPlan plan;
}
