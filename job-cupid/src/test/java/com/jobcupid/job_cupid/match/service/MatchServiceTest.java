package com.jobcupid.job_cupid.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.entity.ApplicationStatus;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.match.dto.MatchResponse;
import com.jobcupid.job_cupid.match.entity.Match;
import com.jobcupid.job_cupid.match.entity.MatchStatus;
import com.jobcupid.job_cupid.match.event.MatchEvent;
import com.jobcupid.job_cupid.match.event.MatchEventPublisher;
import com.jobcupid.job_cupid.match.repository.MatchRepository;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;
import com.jobcupid.job_cupid.swipe.repository.EmployerSwipeRepository;
import com.jobcupid.job_cupid.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock CandidateSwipeRepository candidateSwipeRepository;
    @Mock EmployerSwipeRepository  employerSwipeRepository;
    @Mock ApplicationRepository    applicationRepository;
    @Mock JobRepository            jobRepository;
    @Mock MatchRepository          matchRepository;
    @Mock MatchEventPublisher      matchEventPublisher;

    @InjectMocks MatchService matchService;

    private UUID        candidateId;
    private UUID        employerId;
    private UUID        jobId;
    private UUID        applicationId;
    private Application application;
    private Job         job;

    @BeforeEach
    void setUp() {
        candidateId   = UUID.randomUUID();
        employerId    = UUID.randomUUID();
        jobId         = UUID.randomUUID();
        applicationId = UUID.randomUUID();

        application = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .status(ApplicationStatus.PENDING).appliedAt(OffsetDateTime.now()).build();

        job = Job.builder()
                .id(jobId).employerId(employerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).status(JobStatus.ACTIVE).build();
    }

    @Test
    void evaluateMatchGate_createsMatchAndPublishesEvent_whenAllConditionsMet() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);
        when(employerSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, EmployerSwipeAction.LIKE)).thenReturn(true);
        when(matchRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(false);
        when(applicationRepository.findByCandidateIdAndJobId(candidateId, jobId))
                .thenReturn(Optional.of(application));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        Match saved = Match.builder()
                .id(UUID.randomUUID()).candidateId(candidateId).employerId(employerId)
                .jobId(jobId).applicationId(applicationId).matchedAt(OffsetDateTime.now()).build();
        when(matchRepository.saveAndFlush(any(Match.class))).thenReturn(saved);

        Optional<Match> result = matchService.evaluateMatchGate(candidateId, jobId);

        assertThat(result).isPresent();
        assertThat(result.get().getCandidateId()).isEqualTo(candidateId);
        verify(matchRepository).saveAndFlush(any(Match.class));
        verify(matchEventPublisher).publish(any(MatchEvent.class));
    }

    @Test
    void evaluateMatchGate_returnsEmpty_whenEmployerHasNotLiked() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);
        when(employerSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, EmployerSwipeAction.LIKE)).thenReturn(false);

        Optional<Match> result = matchService.evaluateMatchGate(candidateId, jobId);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).saveAndFlush(any());
        verify(matchEventPublisher, never()).publish(any());
    }

    @Test
    void evaluateMatchGate_returnsEmpty_whenApplicationDoesNotExist() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(false);
        when(employerSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, EmployerSwipeAction.LIKE)).thenReturn(true);

        Optional<Match> result = matchService.evaluateMatchGate(candidateId, jobId);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).saveAndFlush(any());
    }

    @Test
    void evaluateMatchGate_returnsEmpty_whenMatchAlreadyExists() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);
        when(employerSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, EmployerSwipeAction.LIKE)).thenReturn(true);
        when(matchRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);

        Optional<Match> result = matchService.evaluateMatchGate(candidateId, jobId);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).saveAndFlush(any());
    }

    @Test
    void evaluateMatchGate_returnsEmpty_whenSaveThrowsDataIntegrityViolation() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);
        when(employerSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, EmployerSwipeAction.LIKE)).thenReturn(true);
        when(matchRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(false);
        when(applicationRepository.findByCandidateIdAndJobId(candidateId, jobId))
                .thenReturn(Optional.of(application));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(matchRepository.saveAndFlush(any(Match.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Optional<Match> result = matchService.evaluateMatchGate(candidateId, jobId);

        assertThat(result).isEmpty();
        verify(matchEventPublisher, never()).publish(any());
    }

    // ── getMatchesForUser ─────────────────────────────────────────────────────

    @Test
    void getMatchesForUser_returns2Matches_forCandidateWithTwoMatches() {
        List<Match> matches = List.of(
                Match.builder().id(UUID.randomUUID()).candidateId(candidateId).employerId(employerId)
                        .jobId(jobId).applicationId(applicationId)
                        .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build(),
                Match.builder().id(UUID.randomUUID()).candidateId(candidateId).employerId(employerId)
                        .jobId(UUID.randomUUID()).applicationId(UUID.randomUUID())
                        .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        when(matchRepository.findByCandidateId(candidateId, pageable))
                .thenReturn(new PageImpl<>(matches, pageable, 2));

        Page<MatchResponse> result = matchService.getMatchesForUser(candidateId, UserRole.USER, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void getMatchesForUser_returns3Matches_forEmployerWithThreeMatches() {
        List<Match> matches = List.of(
                Match.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID()).employerId(employerId)
                        .jobId(jobId).applicationId(UUID.randomUUID())
                        .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build(),
                Match.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID()).employerId(employerId)
                        .jobId(jobId).applicationId(UUID.randomUUID())
                        .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build(),
                Match.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID()).employerId(employerId)
                        .jobId(jobId).applicationId(UUID.randomUUID())
                        .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        when(matchRepository.findByEmployerId(employerId, pageable))
                .thenReturn(new PageImpl<>(matches, pageable, 3));

        Page<MatchResponse> result = matchService.getMatchesForUser(employerId, UserRole.EMPLOYER, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().get(0).getEmployerId()).isEqualTo(employerId);
    }

    // ── getMatchById ──────────────────────────────────────────────────────────

    @Test
    void getMatchById_returnsMatch_whenUserIsCandidate() {
        UUID matchId = UUID.randomUUID();
        Match match = Match.builder().id(matchId).candidateId(candidateId).employerId(employerId)
                .jobId(jobId).applicationId(applicationId)
                .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchResponse result = matchService.getMatchById(candidateId, matchId);

        assertThat(result.getId()).isEqualTo(matchId);
    }

    @Test
    void getMatchById_throwsResourceNotFoundException_whenUserIsNotParticipant() {
        UUID matchId    = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        Match match = Match.builder().id(matchId).candidateId(candidateId).employerId(employerId)
                .jobId(jobId).applicationId(applicationId)
                .status(MatchStatus.ACTIVE).matchedAt(OffsetDateTime.now()).build();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.getMatchById(outsiderId, matchId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
