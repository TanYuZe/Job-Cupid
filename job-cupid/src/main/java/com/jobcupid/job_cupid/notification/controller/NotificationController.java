package com.jobcupid.job_cupid.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.notification.dto.NotificationPageResponse;
import com.jobcupid.job_cupid.notification.dto.NotificationResponse;
import com.jobcupid.job_cupid.notification.service.NotificationService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(principal.getId(), pageable));
    }

    @PutMapping("/{id}/read")
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(principal.getId(), id));
    }

    @PutMapping("/read-all")
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }
}
