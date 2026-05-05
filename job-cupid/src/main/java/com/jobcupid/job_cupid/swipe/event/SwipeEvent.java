package com.jobcupid.job_cupid.swipe.event;

import java.time.Instant;
import java.util.UUID;

public record SwipeEvent(
        String eventId,
        UUID actorId,
        String actorRole,
        UUID targetId,
        String targetType,
        String action,
        Instant timestamp
) {}
