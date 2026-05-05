package com.jobcupid.job_cupid.notification.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

    private UUID    id;
    private UUID    userId;
    private String  type;
    private String  title;
    private String  body;
    private UUID    referenceId;
    private String  referenceType;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
}
