package com.jobcupid.job_cupid.match.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.match.dto.MatchResponse;
import com.jobcupid.job_cupid.match.entity.Match;
import com.jobcupid.job_cupid.match.event.MatchEvent;
import com.jobcupid.job_cupid.match.event.MatchEventPublisher;
import com.jobcupid.job_cupid.match.repository.MatchRepository;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;
import com.jobcupid.job_cupid.swipe.repository.EmployerSwipeRepository;
import com.jobcupid.job_cupid.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final CandidateSwipeRepository candidateSwipeRepository;
    private final EmployerSwipeRepository  employerSwipeRepository;
    private final ApplicationRepository    applicationRepository;
    private final JobRepository            jobRepository;
    private final MatchRepository          matchRepository;
    private final MatchEventPublisher      matchEventPublisher;

    /**
     * Evaluates the three-condition match gate. If all conditions are met and no
     * match yet exists, creates a match row and publishes a MatchEvent.
     * Idempotent: concurrent duplicate attempts are silently dropped via
     * DataIntegrityViolationException catch (ON CONFLICT DO NOTHING semantics).
     */
    public Optional<Match> evaluateMatchGate(UUID candidateId, UUID jobId) {
        boolean candidateLiked = candidateSwipeRepository
                .existsByCandidateIdAndJobIdAndAction(candidateId, jobId, SwipeAction.LIKE);
        boolean hasApplication = applicationRepository
                .existsByCandidateIdAndJobId(candidateId, jobId);
        boolean employerLiked  = employerSwipeRepository
                .existsByCandidateIdAndJobIdAndAction(candidateId, jobId, EmployerSwipeAction.LIKE);

        if (!candidateLiked || !hasApplication || !employerLiked) {
            return Optional.empty();
        }

        if (matchRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            return Optional.empty();
        }

        Application application = applicationRepository
                .findByCandidateIdAndJobId(candidateId, jobId).orElse(null);
        if (application == null) return Optional.empty();

        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId).orElse(null);
        if (job == null) return Optional.empty();

        try {
            Match match = matchRepository.saveAndFlush(Match.builder()
                    .candidateId(candidateId)
                    .employerId(job.getEmployerId())
                    .jobId(jobId)
                    .applicationId(application.getId())
                    .matchedAt(OffsetDateTime.now())
                    .build());

            matchEventPublisher.publish(new MatchEvent(
                    UUID.randomUUID().toString(),
                    candidateId,
                    job.getEmployerId(),
                    jobId,
                    match.getId(),
                    Instant.now()
            ));

            return Optional.of(match);
        } catch (DataIntegrityViolationException e) {
            log.debug("Match already exists for candidateId={} jobId={} — concurrent creation ignored",
                    candidateId, jobId);
            return Optional.empty();
        }
    }

    public Page<MatchResponse> getMatchesForUser(UUID userId, UserRole role, Pageable pageable) {
        Page<Match> page = role == UserRole.EMPLOYER
                ? matchRepository.findByEmployerId(userId, pageable)
                : matchRepository.findByCandidateId(userId, pageable);
        return page.map(this::toResponse);
    }

    public MatchResponse getMatchById(UUID userId, UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));
        if (!userId.equals(match.getCandidateId()) && !userId.equals(match.getEmployerId())) {
            throw new ResourceNotFoundException("Match not found: " + matchId);
        }
        return toResponse(match);
    }

    private MatchResponse toResponse(Match match) {
        return MatchResponse.builder()
                .id(match.getId())
                .candidateId(match.getCandidateId())
                .employerId(match.getEmployerId())
                .jobId(match.getJobId())
                .applicationId(match.getApplicationId())
                .status(match.getStatus().name())
                .matchedAt(match.getMatchedAt() != null ? match.getMatchedAt().toInstant() : null)
                .build();
    }
}
