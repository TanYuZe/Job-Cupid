package com.jobcupid.job_cupid.shared.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    @Builder.Default
    private final Instant timestamp = Instant.now();

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String traceId;

    // Populated only for validation failures (400)
    private final List<FieldViolation> violations;

    @Getter
    @Builder
    public static class FieldViolation {
        private final String field;
        private final String message;
    }
}
