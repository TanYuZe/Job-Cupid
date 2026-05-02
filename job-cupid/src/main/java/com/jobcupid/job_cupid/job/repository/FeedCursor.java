package com.jobcupid.job_cupid.job.repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import com.jobcupid.job_cupid.job.entity.Job;

import lombok.Value;

/**
 * Opaque keyset cursor — encodes (boost_score, created_at, id) as a
 * URL-safe Base64 string so the client never sees raw pagination state.
 *
 * Format (before encoding): "boostScore|epochMillis|uuid"
 */
@Value
public class FeedCursor {

    int             boostScore;
    OffsetDateTime  createdAt;
    UUID            id;

    public String encode() {
        String raw = boostScore + "|" + createdAt.toInstant().toEpochMilli() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static FeedCursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            return new FeedCursor(
                    Integer.parseInt(parts[0]),
                    Instant.ofEpochMilli(Long.parseLong(parts[1])).atOffset(ZoneOffset.UTC),
                    UUID.fromString(parts[2]));
        } catch (Exception e) {
            return null; // invalid cursor treated as first page
        }
    }

    public static FeedCursor from(Job job) {
        return new FeedCursor(job.getBoostScore(), job.getCreatedAt(), job.getId());
    }
}
