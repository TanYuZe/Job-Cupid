package com.jobcupid.job_cupid.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.notification.dto.NotificationPageResponse;
import com.jobcupid.job_cupid.notification.dto.NotificationResponse;
import com.jobcupid.job_cupid.notification.service.NotificationService;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.JwtAuthenticationFilter;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean NotificationService      notificationService;
    @MockitoBean JwtAuthenticationFilter  jwtAuthenticationFilter;
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

    private NotificationResponse buildResponse(UUID id, boolean isRead) {
        return NotificationResponse.builder()
                .id(id).userId(userId)
                .type("MATCH_CREATED").title("You have a new match!").body("Details here.")
                .isRead(isRead).createdAt(Instant.now())
                .build();
    }

    // ── GET /api/v1/notifications ─────────────────────────────────────────────

    @Test
    void getNotifications_returns200WithItemsAndUnreadCount_forUserWith5Notifications() throws Exception {
        List<NotificationResponse> items = List.of(
                buildResponse(UUID.randomUUID(), false),
                buildResponse(UUID.randomUUID(), false),
                buildResponse(UUID.randomUUID(), false),
                buildResponse(UUID.randomUUID(), true),
                buildResponse(UUID.randomUUID(), true)
        );

        NotificationPageResponse pageResponse = NotificationPageResponse.builder()
                .content(items).pageNumber(0).pageSize(10)
                .totalElements(5).totalPages(1).last(true)
                .unreadCount(3)
                .build();

        when(notificationService.getNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/notifications")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    // ── PUT /api/v1/notifications/{id}/read ───────────────────────────────────

    @Test
    void markAsRead_returns200WithIsReadTrue_whenUnreadNotification() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(notificationService.markAsRead(eq(userId), eq(notifId)))
                .thenReturn(buildResponse(notifId, true));

        mockMvc.perform(put("/api/v1/notifications/{id}/read", notifId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markAsRead_returns200_whenAlreadyRead_isIdempotent() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(notificationService.markAsRead(eq(userId), eq(notifId)))
                .thenReturn(buildResponse(notifId, true));

        mockMvc.perform(put("/api/v1/notifications/{id}/read", notifId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markAsRead_returns404_whenNotificationDoesNotExist() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(notificationService.markAsRead(eq(userId), eq(notifId)))
                .thenThrow(new ResourceNotFoundException("Notification not found: " + notifId));

        mockMvc.perform(put("/api/v1/notifications/{id}/read", notifId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/v1/notifications/read-all ────────────────────────────────────

    @Test
    void markAllAsRead_returns200_forUserWith3UnreadNotifications() throws Exception {
        doNothing().when(notificationService).markAllAsRead(userId);

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }
}
