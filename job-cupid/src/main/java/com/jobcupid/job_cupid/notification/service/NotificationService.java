package com.jobcupid.job_cupid.notification.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.notification.dto.NotificationPageResponse;
import com.jobcupid.job_cupid.notification.dto.NotificationResponse;
import com.jobcupid.job_cupid.notification.entity.Notification;
import com.jobcupid.job_cupid.notification.entity.NotificationType;
import com.jobcupid.job_cupid.notification.repository.NotificationRepository;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(UUID userId, NotificationType type,
            String title, String body, UUID referenceId, String referenceType) {
        return notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());
    }

    public NotificationPageResponse getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        return NotificationPageResponse.builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(Boolean.TRUE);
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType().name())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt() != null ? n.getReadAt().toInstant() : null)
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toInstant() : null)
                .build();
    }
}
