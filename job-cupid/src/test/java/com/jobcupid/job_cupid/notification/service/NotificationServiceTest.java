package com.jobcupid.job_cupid.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.jobcupid.job_cupid.notification.dto.NotificationPageResponse;
import com.jobcupid.job_cupid.notification.dto.NotificationResponse;
import com.jobcupid.job_cupid.notification.entity.Notification;
import com.jobcupid.job_cupid.notification.entity.NotificationType;
import com.jobcupid.job_cupid.notification.repository.NotificationRepository;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;

    @InjectMocks NotificationService notificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private Notification buildNotif(UUID id, boolean isRead) {
        return Notification.builder()
                .id(id).userId(userId)
                .type(NotificationType.MATCH_CREATED)
                .title("You have a new match!").body("Details.")
                .isRead(isRead).createdAt(OffsetDateTime.now())
                .build();
    }

    // ── createNotification ────────────────────────────────────────────────────

    @Test
    void createNotification_savesWithIsReadFalse_whenValidParams() {
        UUID referenceId = UUID.randomUUID();
        Notification saved = buildNotif(UUID.randomUUID(), false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        Notification result = notificationService.createNotification(
                userId, NotificationType.MATCH_CREATED,
                "You have a new match!", "A company liked your application.",
                referenceId, "MATCH");

        assertThat(result.getIsRead()).isFalse();
        verify(notificationRepository).save(argThat(n ->
                n.getUserId().equals(userId)
                        && n.getType() == NotificationType.MATCH_CREATED
                        && !n.getIsRead()));
    }

    // ── getNotifications ──────────────────────────────────────────────────────

    @Test
    void getNotifications_returns5ItemsWithUnreadCount3_forUserWith5Notifications() {
        List<Notification> notifs = List.of(
                buildNotif(UUID.randomUUID(), false),
                buildNotif(UUID.randomUUID(), false),
                buildNotif(UUID.randomUUID(), false),
                buildNotif(UUID.randomUUID(), true),
                buildNotif(UUID.randomUUID(), true)
        );
        Pageable pageable = PageRequest.of(0, 10);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(notifs, pageable, 5));
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(3L);

        NotificationPageResponse result = notificationService.getNotifications(userId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getUnreadCount()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(5);
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test
    void markAsRead_setsIsReadTrue_whenNotificationIsUnread() {
        UUID notifId = UUID.randomUUID();
        Notification unread = buildNotif(notifId, false);
        Notification updated = buildNotif(notifId, true);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(unread));
        when(notificationRepository.save(unread)).thenReturn(updated);

        NotificationResponse result = notificationService.markAsRead(userId, notifId);

        assertThat(result.getIsRead()).isTrue();
        verify(notificationRepository).save(unread);
    }

    @Test
    void markAsRead_isIdempotent_whenNotificationAlreadyRead() {
        UUID notifId = UUID.randomUUID();
        Notification alreadyRead = buildNotif(notifId, true);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(alreadyRead));

        NotificationResponse result = notificationService.markAsRead(userId, notifId);

        assertThat(result.getIsRead()).isTrue();
        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void markAsRead_throwsResourceNotFoundException_whenNotificationDoesNotExist() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notifId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsRead_throwsResourceNotFoundException_whenNotificationBelongsToOtherUser() {
        UUID notifId     = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Notification otherNotif = Notification.builder()
                .id(notifId).userId(otherUserId)
                .type(NotificationType.MATCH_CREATED)
                .title("Title").body("Body").isRead(false).build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(otherNotif));

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notifId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────────

    @Test
    void markAllAsRead_invokesRepositoryBulkUpdate_forUser() {
        when(notificationRepository.markAllAsRead(eq(userId), any(OffsetDateTime.class)))
                .thenReturn(3);

        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsRead(eq(userId), any(OffsetDateTime.class));
    }
}
