package com.jobcupid.job_cupid.subscription.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.stripe.exception.SignatureVerificationException;
import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;
import com.jobcupid.job_cupid.subscription.service.StripeCheckoutService;
import com.jobcupid.job_cupid.subscription.service.StripeWebhookService;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean StripeCheckoutService    stripeCheckoutService;
    @MockitoBean StripeWebhookService     stripeWebhookService;
    @MockitoBean SubscriptionRepository   subscriptionRepository;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean TokenService             tokenService;

    private UUID                                userId;
    private UsernamePasswordAuthenticationToken userAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        UserPrincipal principal = UserPrincipal.of(User.builder()
                .id(userId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(false).isActive(true).isBanned(false)
                .build());

        userAuth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
    }

    // ── POST /api/v1/subscriptions/checkout ───────────────────────────────────

    @Test
    void checkout_returns200WithCheckoutUrl_forAuthenticatedUser() throws Exception {
        when(stripeCheckoutService.createCheckoutUrl(eq(userId), eq(SubscriptionPlan.PREMIUM_MONTHLY)))
                .thenReturn("https://checkout.stripe.com/pay/test_session");

        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PREMIUM_MONTHLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/test_session"));
    }

    // ── POST /api/v1/subscriptions/webhook ────────────────────────────────────

    @Test
    void webhook_returns400_whenStripeSignatureIsInvalid() throws Exception {
        doThrow(new SignatureVerificationException("Invalid signature", "bad_sig"))
                .when(stripeWebhookService).handleEvent(any(), any());

        mockMvc.perform(post("/api/v1/subscriptions/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "bad_sig")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhook_returns200_whenValidStripeEventReceived() throws Exception {
        doNothing().when(stripeWebhookService).handleEvent(any(), any());

        mockMvc.perform(post("/api/v1/subscriptions/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "valid_sig")
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isOk());
    }

    // ── GET /api/v1/subscriptions/me ──────────────────────────────────────────

    @Test
    void getMySubscription_returns200WithStatus_forAuthenticatedUserWithActiveSubscription()
            throws Exception {
        Subscription sub = Subscription.builder()
                .id(UUID.randomUUID()).userId(userId)
                .plan(SubscriptionPlan.PREMIUM_MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(OffsetDateTime.now().minusDays(5))
                .currentPeriodEnd(OffsetDateTime.now().plusDays(25))
                .build();

        when(subscriptionRepository.findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
                userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(sub));

        mockMvc.perform(get("/api/v1/subscriptions/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.plan").value("PREMIUM_MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getMySubscription_returns200WithActiveFalse_forUserWithNoSubscription() throws Exception {
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
                userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/subscriptions/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @TestConfiguration
    static class TestWebMvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
