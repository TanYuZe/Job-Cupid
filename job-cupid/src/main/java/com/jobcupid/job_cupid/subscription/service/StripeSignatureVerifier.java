package com.jobcupid.job_cupid.subscription.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Component
public class StripeSignatureVerifier {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public Event verify(String payload, String sigHeader) throws StripeException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
