package com.jobcupid.job_cupid.subscription.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckoutResponse {

    private String checkoutUrl;
}
