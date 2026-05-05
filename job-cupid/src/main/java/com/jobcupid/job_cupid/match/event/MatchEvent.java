package com.jobcupid.job_cupid.match.event;

import java.time.Instant;
import java.util.UUID;

public record MatchEvent(
        String  eventId,
        UUID    candidateId,
        UUID    employerId,
        UUID    jobId,
        UUID    matchId,
        Instant timestamp
) {}
