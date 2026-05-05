package com.jobcupid.job_cupid.subscription.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;

import jakarta.annotation.PostConstruct;

@Service
public class StripeCheckoutService {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.price.premium-monthly}")
    private String monthlyPriceId;

    @Value("${stripe.price.premium-annual}")
    private String annualPriceId;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @PostConstruct
    void init() {
        Stripe.apiKey = apiKey;
    }

    public String createCheckoutUrl(UUID userId, SubscriptionPlan plan) throws StripeException {
        String priceId = plan == SubscriptionPlan.PREMIUM_ANNUAL ? annualPriceId : monthlyPriceId;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("userId", userId.toString())
                .putMetadata("plan", plan.name())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
