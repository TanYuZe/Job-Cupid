package com.jobcupid.job_cupid.notification.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPageResponse {

    private List<NotificationResponse> content;
    private int                        pageNumber;
    private int                        pageSize;
    private long                       totalElements;
    private int                        totalPages;
    private boolean                    last;
    private long                       unreadCount;
}
