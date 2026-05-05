package com.jobcupid.job_cupid.application.event;

import java.time.Instant;
import java.util.UUID;

public record ApplicationEvent(
        String eventId,
        UUID   candidateId,
        UUID   jobId,
        UUID   applicationId,
        String status,
        Instant timestamp
) {}
